package clients
import javax.inject.{Inject, Singleton, Named}
import notifications.notification._
import io.grpc.ManagedChannelBuilder
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import play.api.inject.ApplicationLifecycle


trait NotificationClient {
  def sendNotification(id: Long, title: String, dueDate: LocalDateTime): Future[NotifyResponse]
}

@Singleton
class NotificationClientImpl @Inject()(config: play.api.Configuration, lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext) extends NotificationClient {
  private val host = config.get[String]("notification.service.host")
  private val port = config.get[Int]("notification.service.port")

  // The gRPC channel is a long-lived connection to the notification service.
  // It is expensive to create, so we create it once and reuse it for the lifetime of the application.
  private val channel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
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
  lifecycle.addStopHook {() => Future.successful(channel.shutdown())}



}