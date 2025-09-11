package controllers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalamock.scalatest.MockFactory
import play.api.test._
import play.api.test.Helpers._
import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import models._
import services.TaskService
import java.time.LocalDateTime
import org.scalatest.BeforeAndAfterEach
import java.time.{LocalDateTime, ZoneOffset}

class TaskControllerSpec extends AnyFlatSpec with Matchers with MockFactory with BeforeAndAfterEach{
      implicit val ec: ExecutionContext = ExecutionContext.global


    implicit val taskFormat = Json.format[Task]
    implicit val taskCreateFormat = Json.format[TaskCreate]
    implicit val taskUpdateFormat = Json.format[TaskUpdate]


  var mockService: TaskService = _
      var controller: TaskController = _

  override def beforeEach(): Unit = {
    mockService = mock[TaskService]
    controller = new TaskController(stubControllerComponents(), mockService)
  }



  object TestServiceData {
    val now = LocalDateTime.now(ZoneOffset.UTC)

    def createTask(id: Long, title: String = "Test Task", dueDate: LocalDateTime = now, status: String = "PENDING") = {
      Task(id, title, dueDate, status, notified = false, createdAt = now, updatedAt = now)
    }
  }


  "TaskController" should "create task and return 200 with ID if valid JSON" in {
    val taskCreate = TaskCreate("Test Task", LocalDateTime.now().plusMinutes(10))

    (mockService.createTask _).expects(taskCreate) returning(Future.successful(1L))

    val request = FakeRequest(POST, "/tasks").withBody(Json.toJson(taskCreate)).withHeaders("Content-Type" -> "application/json")
    val futureResult = controller.createTask()(request)


    status(futureResult) shouldBe CREATED
    contentAsJson(futureResult) shouldBe Json.obj("id" -> 1L)


  }

  it should "return BadRequest when invalid JSON is sent in Create Task" in {
    val badJson = Json.toJson("invalid"->"data")

    val request = FakeRequest(POST, "/tasks").withBody(badJson).withHeaders("Content-Type" -> "application/json")
    val futureResult = controller.createTask()(request)




    status(futureResult) shouldBe BAD_REQUEST
    contentAsString(futureResult) shouldBe "Invalid JSON"


  }


  it should "update a task and return 200 with updated task if valid JSON" in {
    val taskUpdate = TaskUpdate(status = Some("COMPLETED"))
    val updatedTask = TestServiceData.createTask(id = 1L, status = "COMPLETED")


    (mockService.updateTask _).expects(taskUpdate, 1L) returning(Future.successful(Some(updatedTask)))

    val request = FakeRequest(PUT, "/tasks/1").withBody(Json.toJson(taskUpdate)).withHeaders("Content-Type" -> "application/json")
    val futureResult = controller.updateTask(1L)(request)
    status(futureResult) shouldBe OK
    contentAsJson(futureResult) shouldBe Json.toJson(updatedTask)

  }

  it should "return NotFound when invalid id is sent in Update Task" in {
    val id = 2L
    val taskUpdate = TaskUpdate(status = Some("COMPLETED"))

    (mockService.updateTask _).expects(taskUpdate, id) returning(Future.successful(None))
    val request = FakeRequest(PUT, "/tasks/2").withBody(Json.toJson(taskUpdate)).withHeaders("Content-Type" -> "application/json")
    val futureResult = controller.updateTask(id)(request)


    status(futureResult) shouldBe NOT_FOUND
    (contentAsJson(futureResult) \ "error").as[String] shouldBe s"Invalid Task ID: $id"

  }


  it should "get tasks by status" in {
    val task = TestServiceData.createTask(1L)
    (mockService.getTasksByStatus _).expects("PENDING") returning(Future.successful(Seq(task)))

    val request = FakeRequest(GET, "/tasks?status=PENDING")
    val futureResult = controller.getTaskByStatus("PENDING")(request)

    status(futureResult) shouldBe OK
    contentAsJson(futureResult) shouldBe Json.toJson(Seq(task))
  }






}

