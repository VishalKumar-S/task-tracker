import com.google.inject.{AbstractModule, Inject, Provider, Singleton}
import play.api.{Configuration, Environment}
import clients.{NotificationClient, NotificationClientImpl}
import play.api.db.slick.DatabaseConfigProvider
import repositories.TaskRepository
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  /**
   * Configures the dependency injection bindings for the application.
   * This method is used for bindings that cannot be automatically discovered by Guice,
   * such as binding an interface (`NotificationClient`) to a specific implementation (`NotificationClientImpl`).
   */

  override def configure(): Unit =
    bind(classOf[NotificationClient]).to(classOf[NotificationClientImpl])
}

@Singleton
class TaskRepositoryProvider @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
    extends Provider[TaskRepository] {
  override def get(): TaskRepository = {
    val dbConfig = dbConfigProvider.get[JdbcProfile]
    new TaskRepository(dbConfig.profile, dbConfig.db)
  }
}
