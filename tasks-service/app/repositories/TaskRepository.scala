package repositories

import models.{Task, TaskCreate, TaskTableDef, TaskUpdate}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime

@Singleton
class TaskRepository(val profile: JdbcProfile, val db: JdbcProfile#Backend#Database)
                    (implicit ec: ExecutionContext) {

  import profile.api._

  private val tasks =  TaskTableDef.tasks
  def create(task: TaskCreate): Future[Long] = db.run(tasks returning tasks.map(_.id)+=Task(id=0,title = task.title,dueDate = task.dueDate))
  def update(task: Task, id: Long): Future[Option[Task]] = {

    val existingTask = tasks.filter(_.id===id)
    db.run(existingTask.update(task)).flatMap{
      rowsUpdated =>
        if (rowsUpdated>0) db.run(existingTask.result.headOption)
        else Future.successful(None)
    }
  }

  def findByStatus(status: String): Future[Seq[Task]] = db.run(tasks.filter(_.status===status).result)

  def findById(id: Long): Future[Option[Task]] = db.run(tasks.filter(_.id===id).result.headOption)

  def findDueTasks(now: LocalDateTime): Future[Seq[Task]] = {
    val in10Minutes = now.plusMinutes(10)


    val query = tasks.filter { t =>
      t.dueDate >= now && t.dueDate <= in10Minutes && !t.notified
    }
    db.run(query.result)

  }


  def markAsNotified(id: Long): Future[Int] = db.run(tasks.filter(_.id ===id).map(_.notified).update(true))


}
