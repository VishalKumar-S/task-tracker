import com.google.inject.{AbstractModule, Inject, Provider, Singleton}
import play.api.{Configuration, Environment}
import clients.{NotificationClient, NotificationClientImpl}
import play.api.db.slick.DatabaseConfigProvider
import repositories.TaskRepository
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[TaskRepository]).toProvider(classOf[TaskRepositoryProvider])
    bind(classOf[NotificationClient]).to(classOf[NotificationClientImpl])
  }
}

@Singleton
class TaskRepositoryProvider @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends Provider[TaskRepository] {
  override def get(): TaskRepository = {
    val dbConfig = dbConfigProvider.get[JdbcProfile]
    new TaskRepository(dbConfig.profile, dbConfig.db)
  }
}