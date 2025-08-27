package repositories

import models.{Task, TaskCreate, TaskTableDef, TaskUpdate}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

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

//  def findDueTasks(now: LocalDateTime): Future[Seq[Task]] = {
//    val
//    db.run(tasks.filter(_.dueDate=now, d).result)
//  }
//
//  )
}