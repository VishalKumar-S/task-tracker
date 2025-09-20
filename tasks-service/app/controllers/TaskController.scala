package controllers

import play.api.mvc._
import services.TaskService
import models._
import javax.inject._
import scala.concurrent.{Future,ExecutionContext}
import play.api.libs.json._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try




/**
 * The controller for handling all HTTP requests related to tasks.
 * This layer is responsible for request/response validation, JSON serialization,
 * and pass to the TaskService.
 */

@Singleton
class TaskController @Inject()(cc: ControllerComponents, taskService: TaskService)(implicit ec: ExecutionContext) extends AbstractController(cc){

  // --- Constants for Validation ---
  // Defines the only acceptable states for a task's status.
  private val VALID_STATUSES = Set("COMPLETED", "PENDING")

  // Prevents excessively long titles, aligning with potential database constraints.
  private val MAX_CHARACTER_LIMIT = 255

  // Business rule to prevent users from setting due dates far in the future.
  private val MAX_YEARS_FUTURE = 10



  // --- Custom Validation ADT (Algebraic Data Type) ---
  // Using a sealed trait allows for exhaustive pattern matching on validation results,
  // ensuring all outcomes (success or failure) are handled.
  sealed trait ValidationResult
  case object ValidationSuccess extends ValidationResult
  case class ValidationFailure(message: String) extends ValidationResult


  def sanitiseTitle(title: String): String = {
    title.trim.replaceAll("\\s+"," ")
  }


  def errorMessage(message: String) =  Json.obj("error"->message)

  def validateTitle(title: String): ValidationResult = {
    val sanitizedTitle = sanitiseTitle(title)

    if(sanitizedTitle.isEmpty) ValidationFailure("Title cannot be empty")

    else if(sanitizedTitle.length > MAX_CHARACTER_LIMIT){
      ValidationFailure(s"Title cannot exceed $MAX_CHARACTER_LIMIT characters")
    }

    else{
      ValidationSuccess
    }
  }


  def validateDueDate(dueDate: LocalDateTime): ValidationResult = {
    if (dueDate.isAfter(LocalDateTime.now().plusYears(MAX_YEARS_FUTURE))) ValidationFailure(s"Due date cannot be more than ${MAX_YEARS_FUTURE} years in the future")

    else if (dueDate.isBefore(LocalDateTime.now())) ValidationFailure("Due date cannot be in the past.")

    else ValidationSuccess

  }


  def validateStatus(status: String): ValidationResult = {
    if(!VALID_STATUSES.contains(status)) ValidationFailure("Task completion status can either be COMPLETED or PENDING")

    else ValidationSuccess
  }


  def validateTaskCreate(task: TaskCreate): ValidationResult = {
    validateTitle(task.title) match{
      case ValidationSuccess => validateDueDate(task.dueDate)
      case ValidationFailure(message) => ValidationFailure(message)
    }
  }


  def validateTaskUpdate(task: TaskUpdate): ValidationResult = {
    if(task.title.isEmpty && task.dueDate.isEmpty && task.status.isEmpty) ValidationFailure("At least one field must be provided for update")

    else {
      val results = Seq(
        task.title.map(validateTitle),
        task.dueDate.map(validateDueDate),
        task.status.map(validateStatus)
      ).flatten


      results.collectFirst {
        case f: ValidationFailure => f
      }.getOrElse(ValidationSuccess)
      }
    }







  // --- JSON Serialization and Deserialization ---
  // The `implicit` keyword allows Play's JSON library to automatically find
  // and use these helpers for converting between case classes and JSON.

  // Custom Reads for LocalDateTime
  implicit val localDateTimeReads: Reads[LocalDateTime] = Reads[LocalDateTime]{
    json => json.validate[String].flatMap{
      str =>
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        Try(LocalDateTime.parse(str, formatter)).toOption  match {
          case Some(dt) => JsSuccess(dt)
          case None =>  JsError("Invalid JSON format. Please ensure dueDate is in format 'YYYY-MM-DDTHH:MM:SS' (e.g., '2025-12-25T10:30:00')")
        }
    }
  }


  implicit val localDateTimeWrites: Writes[LocalDateTime] = new Writes[LocalDateTime]{
    def writes(d: LocalDateTime): JsValue = JsString(d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
  }

  // Standard formatters for the model classes. Play generates these automatically.
  implicit val taskFormat = Json.format[Task]
  implicit val taskCreateFormat = Json.format[TaskCreate]
  implicit val taskUpdateFormat = Json.format[TaskUpdate]








  def createTask() = Action.async(parse.json) {
    request =>

      (request.body.validate[TaskCreate]) match {
      case JsSuccess(task,_) =>
        val sanitizedTask = task.copy(title = sanitiseTitle(task.title))
        validateTaskCreate(sanitizedTask) match{
        case ValidationSuccess =>

          taskService.createTask(sanitizedTask).map{
          id => Created(Json.obj("id"->id))
        }

        case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
      }


      case JsError(errors) =>
        val errorDetails = errors.map{
          case (path, validationErrors) =>
            val field =  path.toJsonString.replaceAll("^\\.|^/", "")

            val messages = validationErrors.map(_.message).mkString(", ")

            field->messages
        }.toMap

        Future.successful(BadRequest(Json.obj("errors"->errorDetails)))
    }
  }

  def updateTask(id: Long) = Action.async(parse.json){
    request => request.body.validate[TaskUpdate] match {
    case JsSuccess(taskUpdate,_) =>
        val sanitizedUpdate = taskUpdate.copy(
          title = taskUpdate.title.map(sanitiseTitle)
        )


        validateTaskUpdate(sanitizedUpdate) match{
        case ValidationSuccess =>  taskService.updateTask(sanitizedUpdate, id).map{
          case Some(updatedTask) => Ok(Json.toJson(updatedTask))
          case None => NotFound(errorMessage(s"Invalid Task ID: $id"))
        }

        case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
      }

    case JsError(errors) =>
      val errorDetails = errors.map{
        case (path, validationErrors) =>
          val field =  path.toJsonString.replaceAll("^\\.|^/", "")

          val messages = validationErrors.map(_.message).mkString(", ")

          field->messages
      }.toMap

      Future.successful(BadRequest(Json.obj("errors"->errorDetails)))
    }
  }


  def getTaskByStatus(status: String) = Action.async{
    validateStatus(status) match{
      case ValidationSuccess => taskService.getTasksByStatus(status).map{ tasks  => Ok(Json.toJson(tasks))}
      case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
    }
  }


}













