package models
import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}



class UserTableDef(tag: Tag) extends Table[User](tag, "users"){

    implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
      ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
      ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
    )

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def username = column[String]("username")
    def email = column[String]("email")
    def passwordHash = column[String]("password_hash")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")


    override def * = (id, username, email, passwordHash, createdAt, updatedAt) <> (User.tupled, User.unapply)
}


object UserTableDef {
  /**
   * The TableQuery object for the 'users' table.
   * This is the entry point for all database queries involving users.
   */
  val users = TableQuery[UserTableDef]
}