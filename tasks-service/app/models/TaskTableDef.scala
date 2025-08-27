package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}



class TaskTableDef(tag: Tag) extends Table[Task](tag, "tasks"){

    implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
      ldt => Timestamp.valueOf(ldt),
      ts => ts.toLocalDateTime
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