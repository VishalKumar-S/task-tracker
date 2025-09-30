package repositories

import models.{User, UserTableDef, UserCreate, UserLogin}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, ZoneOffset}
import com.typesafe.config.ConfigFactory

/**
 * A repository for data access operations on the `users` table.
 * This class handles all database interactions for User entities.
 *
 * It uses Play's `DatabaseConfigProvider` for idiomatic integration with the application's configuration (application.conf).
 * An auxiliary constructor is provided for dependency-free instantiation in tests.
 *
 * @param dbConfigProvider The provider for the database configuration, injected by Play.
 * @param ec The execution context for asynchronous operations.
 */

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

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




  private val users =  UserTableDef.users

  def create(userCreate: UserCreate, passwordHash: String): Future[User] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)

    val userToInsert = User(id = 0, username = userCreate.username, email = userCreate.email, passwordHash = passwordHash, createdAt = now, updatedAt = now)
    db.run((users returning users.map(_.id) into ((user,id) => user.copy(id = id))) += userToInsert)
  }

  def findByUsername(name: String): Future[Option[User]] = {
    db.run(users.filter(_.username===name).result.headOption)
  }

  def findById(id: Long): Future[Option[User]] = {
    db.run(users.filter(_.id===id).result.headOption)
  }

}
