package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import models._
import repositories.TaskRepository
import clients.NotificationClient
import notifications.notification.NotifyResponse
import org.scalatest.BeforeAndAfterEach

import java.time.{LocalDateTime, ZoneOffset}


class TaskServiceSpec extends AnyFlatSpec with Matchers with MockFactory with BeforeAndAfterEach{

  implicit val ec: ExecutionContext = ExecutionContext.global


  var mockRepo: TaskRepository = _
  var mockNotification: NotificationClient = _
  var mockService: TaskService = _

  override def beforeEach(): Unit = {
    mockRepo = mock[TaskRepository]
    mockNotification = mock[NotificationClient]
    mockService = new TaskService(mockRepo, mockNotification)
  }

  "TaskService" should "create a task and return it's ID" in {
    val task = TaskCreate("Test Task", LocalDateTime.now())

    (mockRepo.create _).expects(task) returning(Future.successful(1L))

    val result =  Await.result(mockService.createTask(task), 2.seconds)

    result shouldBe 1L

  }

  it should "update a task when if exists" in {
    val existingTask = Task(1L, "Test task", LocalDateTime.now())

    val taskUpdate = TaskUpdate(status = Some("COMPLETED"))

    val updatedTask = existingTask.copy(status = "COMPLETED")

    (mockRepo.findById _).expects(1L) returning(Future.successful(Some(existingTask)))
    (mockRepo.update _).expects(updatedTask, 1L) returning(Future.successful(Some(updatedTask)))


    val result = Await.result(mockService.updateTask(taskUpdate, 1L), 2.seconds)

    result.head.status shouldBe "COMPLETED"

  }

  it should "find a task by status if exists and return the task" in {
    val existingTask = Task(1L, "Test task", LocalDateTime.now())
    (mockRepo.findByStatus _).expects("PENDING") returning(Future.successful(Seq(existingTask)))

    val result = Await.result(mockService.getTasksByStatus("PENDING"), 2.seconds)

    result.head shouldBe existingTask

  }


  it should "Call notification client, for tasks due within 10 minutes and update them as Notified" in {
    val dueTaskTime = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5)

    (mockRepo.findDueTasks _).expects(*) returning(Future.successful(Seq(Task(id = 1L, title = "Test Task", dueDate = dueTaskTime))))
    (mockNotification.sendNotification _).expects(1L, "Test Task", dueTaskTime) returning Future.successful(NotifyResponse("SUCCESS"))
    (mockRepo.markAsNotified _).expects(1L) returning Future.successful(1)

    val result = Await.result(mockService.processDueTasks(), 2.seconds)

    result shouldBe (())



  }
}