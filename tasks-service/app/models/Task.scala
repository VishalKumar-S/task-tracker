package models
import java.time.LocalDateTime

/**
 * Represents a full Task entity as it is stored in the database.
 * This is the primary domain model for a task.
 *
 * @param id The unique identifier for the task.
 * @param title The title or description of the task.
 * @param dueDate The date and time the task is due.
 * @param status The current status of the task (e.g., "PENDING", "COMPLETED").
 * @param notified A flag indicating if a notification has been sent for this task.
 * @param createdAt The timestamp when the task was created.
 * @param updatedAt The timestamp when the task was last updated.
 * @param ownerId The ID of the user who owns this task.
 */

case class Task(
  id: Long,
  title: String,
  dueDate: LocalDateTime,
  status: String = "PENDING",
  notified: Boolean = false,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  ownerId: Long
)

/** Data Transfer Object (DTO) for creating a new task via an API request. */
case class TaskCreate(title: String, dueDate: LocalDateTime)

/** Data Transfer Object (DTO) for updating an existing task. All fields are optional. */
case class TaskUpdate(
  title: Option[String] = None,
  dueDate: Option[LocalDateTime] = None,
  status: Option[String] = None
)
