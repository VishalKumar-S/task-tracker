package notifications.models

import slick.jdbc.MySQLProfile.api._
import java.time.{LocalDateTime, ZoneOffset}

case class Notification(
  id: Option[Long] = None,
  taskId: Long,
  taskTitle: String,
  dueDate: LocalDateTime,
  createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
)

class NotificationTable(tag: Tag) extends Table[Notification](tag, "notifications") {
  def id        = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def taskId    = column[Long]("task_id")
  def taskTitle = column[String]("task_title")
  def dueDate   = column[LocalDateTime]("due_date")
  def createdAt = column[LocalDateTime]("created_at")

  def * = (id.?, taskId, taskTitle, dueDate, createdAt).mapTo[Notification]
}

object NotificationTable {
  val notifications = TableQuery[NotificationTable]
}
