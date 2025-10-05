package clients
import javax.inject.{Inject, Singleton, Named}
import notifications.notification._
import io.grpc.ManagedChannelBuilder
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import play.api.inject.ApplicationLifecycle
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.GrpcSslContexts
import java.io.File

trait NotificationClient {
  def sendNotification(id: Long, title: String, dueDate: LocalDateTime): Future[NotifyResponse]
}

@Singleton
class NotificationClientImpl @Inject() (config: play.api.Configuration, lifecycle: ApplicationLifecycle)(implicit
  ec: ExecutionContext
) extends NotificationClient {
  private val host = config.get[String]("notification.service.host")
  private val port = config.get[Int]("notification.service.port")

  // The client's own certificate/key are expected in a local 'certs' directory.
  val certPath = sys.props.getOrElse("certs.path", "certs")

  // The shared root CA certificate is in the project's root 'certs' directory,
  // which is one level up from the service's directory when running locally.
  val caPath = sys.props.getOrElse("ca.path", "../certs")

  private val sslContext = GrpcSslContexts
    .forClient()
    .keyManager(new File(s"$certPath/task.crt"), new File(s"$certPath/task.key"))
    .trustManager(new File(s"$caPath/ca.crt"))
    .build()

  // The gRPC channel is a long-lived connection to the notification service.
  // It is expensive to create, so we create it once and reuse it for the lifetime of the application.
  private val channel = NettyChannelBuilder
    .forAddress(host, port)
    .overrideAuthority("notification-service")
    .sslContext(sslContext)
    .build()

  // The stub is the client-side proxy that allows us to call the remote service's methods.
  private val stub = NotificationServiceGrpc.stub(channel)

  def sendNotification(taskID: Long, taskTitle: String, dueDate: LocalDateTime): Future[NotifyResponse] = {
    val request = NotifyRequest(taskID = taskID, taskTitle = taskTitle, dueDate = dueDate.toString())
    stub.sendNotification(request)
  }

  // Register a stop hook with Play's application lifecycle.
  // This ensures that the gRPC channel is shut down gracefully when the application stops,
  // preventing resource leaks.
  lifecycle.addStopHook(() => Future.successful(channel.shutdown()))

}
