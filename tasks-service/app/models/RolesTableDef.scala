package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}

/**
 * Slick Table Definition for the `roles` table.
 *
 * This class maps the `Role` case class to the `roles` table in the database,
 * defining the table's schema and columns.
 */
class RolesTableDef(tag: Tag) extends Table[Roles](tag, "roles") {

  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
    ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
  )

  def id          = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name        = column[String]("name")
  def description = column[String]("description")
  def createdAt   = column[LocalDateTime]("created_at")
  def updatedAt   = column[LocalDateTime]("updated_at")

  override def * = (id, name, description, createdAt, updatedAt) <> (Roles.tupled, Roles.unapply)
}

object RolesTableDef {

  /** The TableQuery object for the 'roles' table. */
  val roles = TableQuery[RolesTableDef]
}
