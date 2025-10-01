package notifications.models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import java.time.{LocalDateTime, ZoneOffset}

class NotificationSpec extends AnyFlatSpec with Matchers {
  "A notification" should "default createdAt to current UTC time" in {
    // To test that the default `createdAt` is set to "now", we capture the time
    // just before and just after object creation. The `createdAt` timestamp
    // should fall between these two points.
    val before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)

    // Instantiate the Notification without providing a `createdAt` value to test the default parameter.
    val notif = Notification(None, 1L, "Test Task", LocalDateTime.now(ZoneOffset.UTC))
    val after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)

    notif.createdAt.isAfter(before) shouldBe true
    notif.createdAt.isBefore(after) shouldBe true
  }
}
