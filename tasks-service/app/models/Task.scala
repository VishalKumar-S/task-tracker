package models
import java.time.LocalDateTime


case class Task(id: Long, title: String, dueDate: LocalDateTime, status: String = "PENDING", notified: Boolean = false)

case class TaskCreate(title: String, dueDate: LocalDateTime)

case class TaskUpdate(title: Option[String], dueDate: Option[LocalDateTime], status: Option[String], notified: Option[Boolean])