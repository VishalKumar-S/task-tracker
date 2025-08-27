package services

import models._
import repositories.TaskRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//Dependency Injection (DI), managed by Guice in Play.
//
//  You mark classes with @Singleton and put @Inject() in constructors.
//
//Guice sees that UserService needs a UserRepository, and UserRepository needs a Database.
//
//  At runtime, Guice wires everything together.

@Singleton
class TaskService @Inject()(taskRepository: TaskRepository)(implicit ec: ExecutionContext) {
  def createTask(task: TaskCreate): Future[Long] = taskRepository.create(task)

  def updateTask(task: TaskUpdate, id: Long): Future[Option[Task]] = {
    taskRepository.findById(id).flatMap{
      case Some(existingTask) =>
        val updatedTask =
          existingTask.copy(
          title = task.title.getOrElse(existingTask.title),
          dueDate = task.dueDate.getOrElse(existingTask.dueDate),
          status = task.status.getOrElse(existingTask.status),
          notified = task.notified.getOrElse(existingTask.notified)
        )

        taskRepository.update(updatedTask,id)
      case None => Future.successful(None)
    }

  }

  def getTasksByStatus(status: String): Future[Seq[Task]] = taskRepository.findByStatus(status)
}