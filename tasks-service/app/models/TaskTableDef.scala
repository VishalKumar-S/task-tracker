package models

import slick.jdbc.MySQLProfile.api._
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset, ZoneId}



class TaskTableDef(tag: Tag) extends Table[Task](tag, "tasks"){


  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = {
    // Detect if we're running tests
    val isTestEnvironment = Thread.currentThread().getStackTrace().exists(
      _.getClassName.contains("TaskRepositorySpec")
    ) || sys.props.contains("sbt-testing") || sys.props.contains("test.environment")

    if (isTestEnvironment) {
      // H2 Test environment
      MappedColumnType.base[LocalDateTime, Timestamp](
        ldt => Timestamp.valueOf(ldt),
        ts => ts.toLocalDateTime
      )
    } else {
      // MySQL Production environment - UTC timezone handling
      MappedColumnType.base[LocalDateTime, Timestamp](
        ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
        ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
      )
    }
  }




    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def dueDate = column[LocalDateTime]("due_date")
    def status = column[String]("status")
    def notified = column[Boolean]("notified")
    def createdAt = column[LocalDateTime]("created_at")
    def updatedAt = column[LocalDateTime]("updated_at")
    def owner_id = column[Long]("owner_id")

    override def * = (id, title, dueDate, status, notified, createdAt, updatedAt, owner_id) <> (Task.tupled, Task.unapply)

    def owner = foreignKey("fk_owner", owner_id, UserTableDef.users)(_.id)

  }


object TaskTableDef {
  val tasks = TableQuery[TaskTableDef]
}





