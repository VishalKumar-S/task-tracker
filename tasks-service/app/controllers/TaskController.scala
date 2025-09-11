package controllers

import play.api.mvc._
import services.TaskService
import models._
import javax.inject._
import scala.concurrent.{Future,ExecutionContext}
import play.api.libs.json._
import java.time.LocalDateTime




/**
 * The controller for handling all HTTP requests related to tasks.
 * This layer is responsible for request/response validation, JSON serialization,
 * and pass to the TaskService.
 */

@Singleton
class TaskController @Inject()(cc: ControllerComponents, taskService: TaskService)(implicit ec: ExecutionContext) extends AbstractController(cc){

  // Implicit JSON formatters for serializing/deserializing Task-related case classes.
  implicit val taskFormat = Json.format[Task]
  implicit val taskCreateFormat = Json.format[TaskCreate]
  implicit val taskUpdateFormat = Json.format[TaskUpdate]

  def createTask() = Action.async(parse.json) {
    request => (request.body.validate[TaskCreate]).map{
      task => {
        if(task.dueDate.isBefore(LocalDateTime.now())){
          Future.successful(BadRequest(Json.obj("error" -> "Due date cannot be in the past.")))
        }

        else{
          taskService.createTask(task).map{
            id => Created(Json.obj("id"->id))
          }
        }
      }
    }.getOrElse{
      Future.successful(BadRequest("Invalid JSON"))
    }
  }


  def updateTask(id: Long) = Action.async(parse.json){
    request => request.body.validate[TaskUpdate].map{

      taskUpdate => {
        val isDueTaskInvalid = taskUpdate.dueDate.exists(_.isBefore(LocalDateTime.now()))
        if(isDueTaskInvalid){
            Future.successful(BadRequest(Json.obj("error" -> "Due date cannot be in the past.")))
        }

        else if (taskUpdate.status.exists(s => !Set("COMPLETED", "PENDING").contains(s))){
           Future.successful(BadRequest(Json.obj("error" -> "Task completion status can either be COMPLETED or PENDING")))
        }

        else{
          taskService.updateTask(taskUpdate, id).map{
            case Some(updatedTask) => Ok(Json.toJson(updatedTask))
            case None => NotFound(Json.obj("error"->s"Invalid Task ID: $id"))
          }
        }
      }
    }.getOrElse{
      Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON format for updating a task")))
    }
  }


  def getTaskByStatus(status: String) = Action.async{
    if (!Set("COMPLETED", "PENDING").contains(status)){
      Future.successful(BadRequest(Json.obj("error" -> "Task completion status can either be COMPLETED or PENDING")))
    }

    else {taskService.getTasksByStatus(status).map{ tasks  => Ok(Json.toJson(tasks))}}
  }


}














