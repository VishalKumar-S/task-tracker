package repositories

import models.{Task, TaskCreate, TaskTableDef, TaskUpdate}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, ZoneOffset}


@Singleton
class TaskRepository(val profile: JdbcProfile, val db: JdbcProfile#Backend#Database)
                    (implicit ec: ExecutionContext) {

  import profile.api._

  private val tasks =  TaskTableDef.tasks
  def create(task: TaskCreate): Future[Long] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    db.run(tasks returning tasks.map(_.id)+=Task(id=0, title = task.title, dueDate = task.dueDate, createdAt = now, updatedAt = now))
  }

  def update(task: Task, id: Long): Future[Option[Task]] = {
    val query = tasks.filter(_.id===id)

    // IMPORTANT: The `createdAt` field is intentionally omitted from this update.
    // The custom `LocalDateTime` mapping in `TaskTableDef` assumes the object's time is in 'Asia/Kolkata'
    // before converting it to a UTC timestamp for the database. If we were to include `createdAt`
    // in the update, the existing UTC value read from the DB would be misinterpreted as IST and
    // incorrectly converted back to a new, different UTC value.
    // By excluding it, we ensure the original creation timestamp is never altered.    val updateAction = query.map(t=> (t.title, t.dueDate, t.status, t.notified, t.updatedAt)).update((task.title, task.dueDate, task.status, task.notified, task.updatedAt))
    val updateAction = query.map(t => (t.title, t.dueDate, t.status, t.notified, t.updatedAt)).update((task.title, task.dueDate, task.status, task.notified, task.updatedAt))
    db.run(updateAction).flatMap{
      rowsUpdated => if(rowsUpdated > 0) db.run(query.result.headOption)
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
