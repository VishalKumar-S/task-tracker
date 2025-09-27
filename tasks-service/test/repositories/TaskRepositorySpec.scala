package repositories

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import slick.jdbc.H2Profile
import scala.concurrent.{Await, ExecutionContext, Future}
import models._
import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.duration._
import slick.jdbc.H2Profile.api._
import java.time.temporal.ChronoUnit

class TaskRepositorySpec extends AnyFlatSpec with Matchers{

  sys.props.put("test.environment", "true")
  implicit val ec: ExecutionContext = ExecutionContext.global
  val db = H2Profile.api.Database.forConfig("h2mem1")
  val repo = new TaskRepository(H2Profile, db)

  override def withFixture(test: NoArgTest) = {
    // Proper test isolation: drop and recreate table for each test
    Await.result(db.run(TaskTableDef.tasks.schema.dropIfExists), 2.seconds)
    Await.result(db.run(TaskTableDef.tasks.schema.createIfNotExists), 2.seconds)
    super.withFixture(test)
  }

"TaskRepository" should "create tasks and return it's ID" in {
  val resultID = Await.result(repo.create(TaskCreate("Test Task", LocalDateTime.now(ZoneOffset.UTC))), 2.seconds)

  resultID shouldBe 1L
}

  it should "update a task if exists" in {
    val id = Await.result(repo.create(TaskCreate("Test Task", LocalDateTime.now(ZoneOffset.UTC))), 2.seconds)

    val now = LocalDateTime.now(ZoneOffset.UTC)
    val updatedTask = Await.result(repo.update(Task(id, "Updated Task", dueDate =  now, createdAt = now, updatedAt = now), id), 2.seconds)

    updatedTask.head.title shouldBe "Updated Task"
  }


  it should "find a task by ID if exists and return the task" in {
    val id = Await.result(repo.create(TaskCreate("Test Task", LocalDateTime.now(ZoneOffset.UTC))), 2.seconds)
    val taskReturned = Await.result(repo.findById(id), 2.seconds)

    taskReturned.head.title shouldBe "Test Task"
  }



  it should "find tasks by the status of completion and return those tasks" in {
    Await.result(repo.create(TaskCreate("Test Task", LocalDateTime.now(ZoneOffset.UTC))), 2.seconds)

      val pendingTasks = Await.result(repo.findByStatus("PENDING"), 2.seconds)
      pendingTasks.map(_.status).distinct shouldBe Seq("PENDING")

  }


  it should "find tasks which will be due within 10 minutes and return those tasks" in {
    val now = LocalDateTime.now(ZoneOffset.UTC)
    val in8Minutes = now.plusMinutes(8)


    Await.result(repo.create(TaskCreate("Task Due Soon", in8Minutes)), 2.seconds)
    Await.result(repo.create(TaskCreate("Task Due Later", now.plusHours(1))), 2.seconds)


    val dueTasks = Await.result(repo.findDueTasks(now), 2.seconds)
    dueTasks.head.title shouldBe "Task Due Soon"

  }



  it should "Update task as Notified" in {

    val taskID = Await.result(repo.create(TaskCreate("Mark as Notified", LocalDateTime.now(ZoneOffset.UTC))), 2.seconds)

    val updatedRows = Await.result(repo.markAsNotified(taskID), 2.seconds)

    updatedRows shouldBe 1

  }


}