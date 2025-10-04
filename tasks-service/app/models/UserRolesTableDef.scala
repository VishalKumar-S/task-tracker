package models
import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}

/**
 * Slick Table Definition for the `user_roles` join table.
 *
 * This class maps the `UserRoles` case class to the `user_roles` table in the database,
 * defining the table's schema, columns, composite primary key, and foreign key relationships.
 */

class UserRolesTableDef(tag: Tag) extends Table[UserRoles](tag, "user_roles") {

  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
    ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
  )

  // Columns
  def userId    = column[Long]("user_id")
  def roleId    = column[Long]("role_id")
  def createdAt = column[LocalDateTime]("created_at")

  // Primary key (composite key of user_id + role_id)
  def pk = primaryKey("pk_user_roles", (userId, roleId))

  // Foreign key to users table
  def userFk = foreignKey("fk_user_roles_user", userId, UserTableDef.users)(_.id, onDelete = ForeignKeyAction.Cascade)

  // Foreign key to roles table
  def roleFk = foreignKey("fk_user_roles_role", roleId, RolesTableDef.roles)(_.id, onDelete = ForeignKeyAction.Cascade)

  // Default projection (mapping)
  override def * = (userId, roleId, createdAt) <> (UserRoles.tupled, UserRoles.unapply)
}

/**
 * Companion object to provide a TableQuery for easy access in repositories.
 */
object UserRolesTableDef {
  val userRoles = TableQuery[UserRolesTableDef]
}
