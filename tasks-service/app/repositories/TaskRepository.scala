package repositories

import models.{Task, TaskCreate, TaskTableDef, TaskUpdate}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, ZoneOffset}
import com.typesafe.config.ConfigFactory

/**
 * A repository for data access operations on the `tasks` table.
 * This class handles all database interactions for Task entities.
 *
 * It uses Play's `DatabaseConfigProvider` for idiomatic integration with the application's configuration (application.conf).
 * An auxiliary constructor is provided for dependency-free instantiation in tests.
 *
 * @param dbConfigProvider The provider for the database configuration, injected by Play.
 * @param ec The execution context for asynchronous operations.
 */

@Singleton
class TaskRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._



  // Auxiliary constructor for testing purposes, allowing direct injection of a test database.
  def this(testProfile: JdbcProfile, testDb: JdbcProfile#Backend#Database)(implicit ec: ExecutionContext) = {
    this(new DatabaseConfigProvider {
      override def get[P <: slick.basic.BasicProfile]: slick.basic.DatabaseConfig[P] = {
        new slick.basic.DatabaseConfig[P] {
          override val profile: P = testProfile.asInstanceOf[P]
          override val db: P#Backend#Database = testDb.asInstanceOf[P#Backend#Database]
          override val config: com.typesafe.config.Config = ConfigFactory.empty()

          // extra members required by DatabaseConfig
          override val driver: P = profile
          override def profileIsObject: Boolean = true
          override def profileName: String = testProfile.getClass.getName
        }
      }
    })
  }




  private val tasks =  TaskTableDef.tasks
  def create(task: TaskCreate): Future[Long] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    db.run(tasks returning tasks.map(_.id)+=Task(id=0, title = task.title, dueDate = task.dueDate, createdAt = now, updatedAt = now))
  }

  def update(task: Task, id: Long): Future[Option[Task]] = {
    val query = tasks.filter(_.id===id)

    // IMPORTANT: The `createdAt` field is intentionally omitted from this update.
    // Reason: `createdAt` should never be modified after a task is created - it represents
    // the immutable creation timestamp. Only `updatedAt` should change during updates.
    // By excluding `createdAt` from the update query, we ensure it remains exactly as originally stored.
    val updateAction = query.map(t => (t.title, t.dueDate, t.status, t.notified, t.updatedAt)).update((task.title, task.dueDate, task.status, task.notified, task.updatedAt))
    db.run(updateAction).flatMap{
      rowsUpdated => if(rowsUpdated > 0) db.run(query.result.headOption)
      else Future.successful(None)
    }
  }

  def findByStatus(status: String): Future[Seq[Task]] = db.run(tasks.filter(_.status===status).result)

  def findById(id: Long): Future[Option[Task]] = db.run(tasks.filter(_.id===id).result.headOption)



  /**
   * Finds all tasks that are due within the next 10 minutes and have not yet been notified.
   * This is used by the background scheduler to identify tasks needing a notification.
   * @param now The current UTC time to calculate the due window from.
   * Note: All datetime comparisons are done in UTC since that's how data is stored.
   */

  def findDueTasks(now: LocalDateTime): Future[Seq[Task]] = {
    val in10Minutes = now.plusMinutes(10)


    val query = tasks.filter { t =>
      t.dueDate >= now && t.dueDate <= in10Minutes && !t.notified
    }
    db.run(query.result)

  }

  def markAsNotified(id: Long): Future[Int] = db.run(tasks.filter(_.id ===id).map(_.notified).update(true))


}
