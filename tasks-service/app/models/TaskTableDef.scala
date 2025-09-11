package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}



class TaskTableDef(tag: Tag) extends Table[Task](tag, "tasks"){

      // IMPORTANT: This custom mapping handles the conversion between the database's `TIMESTAMP`
      // and the application's `LocalDateTime`. It has a critical behavior:
      //
      // 1. On WRITE: It assumes the `LocalDateTime` object represents a time in "Asia/Kolkata"
      //    and converts it to a UTC timestamp before storing it in the database.
      // 2. On READ: It reads the UTC timestamp from the database and creates a `LocalDateTime`
      //    object representing that UTC time.
      //
      // This asymmetry is why `createdAt` must be excluded from update operations to avoid
      // incorrectly re-interpreting a UTC value as an IST value.

      implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](

      // Write path: Assume IST -> Convert to UTC
      ldt => Timestamp.from(ldt.atZone(ZoneId.of("Asia/Kolkata")).toInstant()),

      // Read path:  Read UTC -> Create UTC-based LocalDateTime
      ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
    )

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def dueDate = column[LocalDateTime]("due_date")
    def status = column[String]("status")
    def notified = column[Boolean]("notified")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")


    override def * = (id, title, dueDate, status, notified, createdAt, updatedAt) <> (Task.tupled, Task.unapply)
  }


object TaskTableDef {
  val tasks = TableQuery[TaskTableDef]
}