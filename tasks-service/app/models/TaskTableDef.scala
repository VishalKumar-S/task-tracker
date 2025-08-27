package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDate

class TaskTableDef(tag: Tag) extends Table[Task](tag, "tasks"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def dueDate = column[LocalDate]("due_date")
  def status = column[String]("status")
  def notified = column[Boolean]("notified")

  override def * = (id, title, dueDate, status, notified) <> (Task.tupled, Task.unapply)
}

object TaskTableDef {
  val tasks = TableQuery[TaskTableDef]
}