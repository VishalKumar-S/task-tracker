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
class TaskService @Inject()(taskRepository: TaskRepository, notificationClient: NotificationClient)(implicit ec: ExecutionContext) {
  def createTask(task: TaskCreate): Future[Long] = taskRepository.create(task)

  def updateTask(task: TaskUpdate, id: Long): Future[Option[Task]] = {
    taskRepository.findById(id).flatMap{
      case Some(existingTask) =>
        val updatedTask =
          existingTask.copy(
            title = task.title.getOrElse(existingTask.title),
            dueDate = task.dueDate.getOrElse(existingTask.dueDate),
            status = task.status.getOrElse(existingTask.status),
            updatedAt = LocalDateTime.now(ZoneOffset.UTC)
          )

        taskRepository.update(updatedTask,id)
      case None => Future.successful(None)
    }

  }

  def getTasksByStatus(status: String): Future[Seq[Task]] = taskRepository.findByStatus(status)

  /**
   * Orchestrates the process of handling tasks that are due soon.
   * It finds due tasks, sends a notification for each one via the gRPC client,
   * and then marks them as notified in the database.
   */
  def processDueTasks(): Future[Unit] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    taskRepository.findDueTasks(now).flatMap {
      dueTasks =>
        val notificationFutures = dueTasks.map {
          dueTask =>
            for {
              response <- notificationClient.sendNotification(dueTask.id, dueTask.title, dueTask.dueDate)
              _ = println(s"Task id: ${dueTask.id} status: ${response.status}")
              _ <- taskRepository.markAsNotified(dueTask.id)
            } yield ()
        }
        Future.sequence(notificationFutures).map(_ => ())
    }
  }
}