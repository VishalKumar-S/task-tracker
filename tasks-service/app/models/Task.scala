package models
import java.time.LocalDate


case class Task(id: Long, title: String, dueDate: LocalDate, status: String = "PENDING", notified: Boolean = false)

case class TaskCreate(title: String, dueDate: LocalDate)

case class TaskUpdate(title: Option[String], dueDate: Option[LocalDate], status: Option[String], notified: Option[Boolean])