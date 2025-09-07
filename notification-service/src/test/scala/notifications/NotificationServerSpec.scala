package notifications

import notifications.notification._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import scala.concurrent.{Await, ExecutionContext, Future}
import notifications.models._
import java.time.{LocalDateTime, ZoneOffset}
import notifications.repositories.NotificationRepository
import scala.concurrent.{Await, ExecutionContext, Future}


class NotificationServerSpec extends AnyFlatSpec with Matchers with MockFactory{
  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockRepo = mock[NotificationRepository]

  val taskId = 1L
  val taskTitle = "Test Task"
  val dueDate = LocalDateTime.now(ZoneOffset.UTC)


  val server =  new NotificationServiceImpl(mockRepo)


  (mockRepo.create _).expects(where {notif: Notification =>
    notif.taskId == taskId &&
      notif.taskTitle == taskTitle &&
      notif.dueDate == dueDate
  }).returning(Future.successful(Notification(Some(1L), taskId, taskTitle, dueDate)))



  val response = server.sendNotification(NotifyRequest(taskId, taskTitle, dueDate.toString()))
  "NotificationServiceImpl" should "return Future.Success when repo.create successfully saves the notification and returns the notification object" in {



    val result = Await.result(response, scala.concurrent.duration.Duration.Inf)

    result.status shouldBe "SUCCESS"

  }

}