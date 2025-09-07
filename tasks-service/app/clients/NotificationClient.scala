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

  private val channel = ManagedChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .build()

  private val stub = NotificationServiceGrpc.stub(channel)

  def sendNotification(taskID: Long, taskTitle: String, dueDate: LocalDateTime): Future[NotifyResponse] = {
    val request = NotifyRequest(taskID = taskID, taskTitle = taskTitle, dueDate = dueDate.toString())
    stub.sendNotification(request)
  }

  lifecycle.addStopHook {() => Future.successful(channel.shutdown())}



}