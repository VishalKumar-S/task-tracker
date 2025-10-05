package services

import models._
import repositories.TaskRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, ZoneOffset}
import clients.NotificationClient

/**
 * The primary business logic layer for task-related operations.
 * This service coordinates interactions between the database (via TaskRepository)
 * and other external services (like NotificationClient).
 */
@Singleton
class TaskService @Inject() (taskRepository: TaskRepository, notificationClient: NotificationClient)(implicit
  ec: ExecutionContext
) {
  def createTask(task: TaskCreate, ownerId: Long): Future[Long] = taskRepository.create(task, ownerId)

  def updateTask(task: TaskUpdate, id: Long, ownerId: Long): Future[Option[Task]] =
    taskRepository.findByIdForOwner(id, ownerId).flatMap {
      case Some(existingTask) =>
        val updatedTask =
          existingTask.copy(
            title = task.title.getOrElse(existingTask.title),
            dueDate = task.dueDate.getOrElse(existingTask.dueDate),
            status = task.status.getOrElse(existingTask.status),
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
          )

        taskRepository.updateForOwner(updatedTask, id, ownerId)
      case None => Future.successful(None)
    }

  def updateAnyTask(task: TaskUpdate, id: Long): Future[Option[Task]] =
    taskRepository.findByIdAny(id).flatMap {
      case Some(existingTask) =>
        val updatedTask =
          existingTask.copy(
            title = task.title.getOrElse(existingTask.title),
            dueDate = task.dueDate.getOrElse(existingTask.dueDate),
            status = task.status.getOrElse(existingTask.status),
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
          )

        taskRepository.updateAny(updatedTask, id)
      case None => Future.successful(None)
    }

  def getTasksByStatus(status: String, ownerId: Long): Future[Seq[Task]] =
    taskRepository.findByStatusForOwner(status, ownerId)

  def getTasksByAnyStatus(status: String): Future[Seq[Task]] = taskRepository.findByStatusAny(status)

  /**
   * Orchestrates the process of handling tasks that are due soon.
   * It finds due tasks, sends a notification for each one via the gRPC client,
   * and then marks them as notified in the database.
   */
  def processDueTasks(): Future[Unit] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    taskRepository.findDueTasks(now).flatMap { dueTasks =>
      val notificationFutures = dueTasks.map { dueTask =>
        val notificationAttempt = for {
          response <- notificationClient.sendNotification(dueTask.id, dueTask.title, dueTask.dueDate)
          _         = println(s"Task id: ${dueTask.id} status: ${response.status}")
          _        <- taskRepository.markAsNotified(dueTask.id)
        } yield ()

        notificationAttempt.recover {
          case e: Exception =>
            println(s"Error sending notification for task ${dueTask.id}: ${e.getMessage}")
        }
      }
      Future.sequence(notificationFutures).map(_ => ())
    }
  }
}
