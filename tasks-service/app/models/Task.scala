package models
import java.time.LocalDateTime


case class Task(id: Long, title: String, dueDate: LocalDateTime, status: String = "PENDING", notified: Boolean = false, createdAt: LocalDateTime, updatedAt: LocalDateTime)

case class TaskCreate(title: String, dueDate: LocalDateTime)

case class TaskUpdate(title: Option[String] = None, dueDate: Option[LocalDateTime] = None, status: Option[String] = None)