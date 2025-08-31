package notifications.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.{LocalDateTime, ZoneOffset}

class NotificationSpec extends AnyFlatSpec with Matchers {
  "A notification" should "dafault createdAt to current UTC time" in {
    val before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)
    val notif = Notification(None, 1L, "Test Task", LocalDateTime.now(ZoneOffset.UTC))
    val after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)

    notif.createdAt.isAfter(before) shouldBe true
    notif.createdAt.isBefore(after) shouldBe true
  }
}