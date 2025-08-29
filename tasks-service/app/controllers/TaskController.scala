package controllers

import play.api.mvc._
import services.TaskService
import models._
import javax.inject._
import scala.concurrent.{Future,ExecutionContext}
import play.api.libs.json._
import java.time.LocalDateTime

@Singleton
class TaskController @Inject()(cc: ControllerComponents, taskService: TaskService)(implicit ec: ExecutionContext) extends AbstractController(cc){

  implicit val taskFormat = Json.format[Task]
  implicit val taskCreateFormat = Json.format[TaskCreate]
  implicit val taskUpdateFormat = Json.format[TaskUpdate]

  def createTask() = Action.async(parse.json) {
    request => (request.body.validate[TaskCreate]).map{
      task => taskService.createTask(task).map{
        id => Ok(Json.obj("id"->id))
      }
    }.getOrElse{
      Future.successful(BadRequest("Invalid JSON"))
    }
  }


  def updateTask(id: Long) = Action.async(parse.json){
    request => request.body.validate[TaskUpdate].map{
      taskUpdate => taskService.updateTask(taskUpdate, id).map{
        case Some(updatedTask) =>  Ok(Json.toJson(updatedTask))
        case None => NotFound(Json.obj("error"->s"Invalid Task ID: $id"))
      }
    }.getOrElse{
      Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON format for updating a task")))
    }
  }


  def getTaskByStatus(status: String) = Action.async{
    taskService.getTasksByStatus(status).map{ tasks  => Ok(Json.toJson(tasks))}
  }
}














