package notifications

import notifications.notification._
import io.grpc.{Server, ServerBuilder}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

class NotificationServiceImpl extends NotificationServiceGrpc.NotificationService {
  override def sendNotification(request: NotifyRequest): Future[NotifyResponse] = {

    val utcDueDate = LocalDateTime.parse(request.dueDate)
    val utcZonedDateTime = ZonedDateTime.of(utcDueDate, ZoneId.of("UTC"))
    val istZonedDateTime = utcZonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))


    println(s"Task due soon task id: ${request.taskID}, task title: ${request.taskTitle} due on ${istZonedDateTime}")
    Future.successful(NotifyResponse(status = "SUCCESS"))
  }
}

object NotificationServer {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global

    val server = ServerBuilder
      .forPort(50051) // gRPC runs on port 50051 by convention
      .addService(NotificationServiceGrpc.bindService(new NotificationServiceImpl, ec))
      .build().start()

    println("NotificationService gRPC server started on port 50051")

    server.awaitTermination()
  }
}
