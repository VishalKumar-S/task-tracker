package notifications.repositories

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.H2Profile
import scala.concurrent.{Await, ExecutionContext, Future}
import notifications.models._
import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.duration._
import slick.jdbc.H2Profile.api._

class NotificationRepositorySpec extends AnyFlatSpec with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val db   = H2Profile.api.Database.forConfig("h2mem1")
  val repo = new NotificationRepository(H2Profile, db)

  override def withFixture(test: NoArgTest) = {
    Await.result(db.run(NotificationTable.notifications.schema.createIfNotExists), 2.seconds)
    super.withFixture(test)
  }

  // FlatSpec Template

  "NotificationRepository.create" should "create a new notification record and returning the record with auto-generated ID" in {
    val notify = Notification(None, 1L, "Test Task", LocalDateTime.now(ZoneOffset.UTC))

    val result = Await.result(repo.create(notify), 2.seconds)

    result.id should not be empty
    result.taskTitle shouldBe "Test Task"

  }

  it should "Assign different id's for multiple inserts" in {
    val notify1 = Notification(None, 1L, "Test Task 1", LocalDateTime.now(ZoneOffset.UTC))
    val notify2 = Notification(None, 2L, "Test Task 2", LocalDateTime.now(ZoneOffset.UTC))

    val result1 = Await.result(repo.create(notify1), 2.seconds)
    val result2 = Await.result(repo.create(notify2), 2.seconds)

    result1.id should not be result2.id

  }

}
