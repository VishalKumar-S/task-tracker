package notifications

import notifications.notification._
import io.grpc.{Server, ServerBuilder}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import org.flywaydb.core.Flyway
import com.typesafe.config.ConfigFactory
import notifications.models._
import notifications.repositories._
import slick.jdbc.MySQLProfile.api._


class NotificationServiceImpl(repository: NotificationRepository)(implicit ec: ExecutionContext) extends NotificationServiceGrpc.NotificationService {
  override def sendNotification(request: NotifyRequest): Future[NotifyResponse] = {

    val utcDueDate = LocalDateTime.parse(request.dueDate)
    val utcZonedDateTime = ZonedDateTime.of(utcDueDate, ZoneId.of("UTC"))
    val istZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))


    val newNotification = Notification(taskId = request.taskID, taskTitle = request.taskTitle, dueDate = utcDueDate)

    repository.create(newNotification).map{
      savedNotification =>
        println(s"Saved notification ID ${savedNotification.id.getOrElse(0)} for task '${savedNotification.taskTitle}' due on ${istZonedDateTime}")
        NotifyResponse(status = "SUCCESS")

    }

  }
}






object NotificationServer {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    val config = ConfigFactory.load()
    val url = config.getString("db.default.url")
    val user = config.getString("db.default.user")
    val password = config.getString("db.default.password")


    val db = Database.forConfig("db.default", config)

    val flyway = Flyway.configure()
      .dataSource(url, user, password)
      .load()



    flyway.migrate()



    val server = ServerBuilder
      .forPort(50051)
      .addService(NotificationServiceGrpc.bindService(new NotificationServiceImpl(new NotificationRepository(db)), ec))
      .build().start()

    println("NotificationService gRPC server started on port 50051")

    server.awaitTermination()
  }
}










