package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}



class TaskTableDef(tag: Tag) extends Table[Task](tag, "tasks"){

      implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
      ldt => Timestamp.from(ldt.atZone(ZoneId.of("Asia/Kolkata")).toInstant()),
      ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
    )

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def dueDate = column[LocalDateTime]("due_date")
    def status = column[String]("status")
    def notified = column[Boolean]("notified")

    override def * = (id, title, dueDate, status, notified) <> (Task.tupled, Task.unapply)
  }


object TaskTableDef {
  val tasks = TableQuery[TaskTableDef]
}