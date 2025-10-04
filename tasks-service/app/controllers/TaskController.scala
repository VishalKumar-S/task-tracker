package controllers

import play.api.mvc._
import services.TaskService
import models._
import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import scala.util.Try
import actions.AuthenticatedAction

/**
 * The controller for handling all HTTP requests related to tasks.
 * This layer is responsible for request/response validation, JSON serialization,
 * and pass to the TaskService.
 */

@Singleton
class TaskController @Inject() (
  cc: ControllerComponents,
  taskService: TaskService,
  authenticatedAction: AuthenticatedAction
)(implicit ec: ExecutionContext)
    extends AbstractController(cc) {

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
  case object ValidationSuccess                 extends ValidationResult
  case class ValidationFailure(message: String) extends ValidationResult

  def sanitiseTitle(title: String): String =
    title.trim.replaceAll("\\s+", " ")

  def errorMessage(message: String) = Json.obj("error" -> message)

  def validateTitle(title: String): ValidationResult = {
    val sanitizedTitle = sanitiseTitle(title)

    if (sanitizedTitle.isEmpty) ValidationFailure("Title cannot be empty")
    else if (sanitizedTitle.length > MAX_CHARACTER_LIMIT) {
      ValidationFailure(s"Title cannot exceed $MAX_CHARACTER_LIMIT characters")
    } else {
      ValidationSuccess
    }
  }

  def validateDueDate(dueDate: LocalDateTime): ValidationResult = {

    val nowUtc = LocalDateTime.now(ZoneOffset.UTC)

    if (dueDate.isAfter(nowUtc.plusYears(MAX_YEARS_FUTURE)))
      ValidationFailure(s"Due date cannot be more than ${MAX_YEARS_FUTURE} years in the future")
    else if (dueDate.isBefore(nowUtc)) ValidationFailure(s"Due date cannot be in the past.")
    else ValidationSuccess

  }

  def validateStatus(status: String): ValidationResult =
    if (!VALID_STATUSES.contains(status)) ValidationFailure("Task completion status can either be COMPLETED or PENDING")
    else ValidationSuccess

  def validateTaskCreate(task: TaskCreate): ValidationResult =
    validateTitle(task.title) match {
      case ValidationSuccess          => validateDueDate(task.dueDate)
      case ValidationFailure(message) => ValidationFailure(message)
    }

  def validateTaskUpdate(task: TaskUpdate): ValidationResult =
    if (task.title.isEmpty && task.dueDate.isEmpty && task.status.isEmpty)
      ValidationFailure("At least one field must be provided for update")
    else {
      val results = Seq(
        task.title.map(validateTitle),
        task.dueDate.map(validateDueDate),
        task.status.map(validateStatus)
      ).flatten

      results.collectFirst { case f: ValidationFailure => f }.getOrElse(ValidationSuccess)
    }

  // --- JSON Serialization and Deserialization ---
  // The `implicit` keyword allows Play's JSON library to automatically find
  // and use these helpers for converting between case classes and JSON.

  // Custom Reads for LocalDateTime
  implicit val localDateTimeReads: Reads[LocalDateTime] = Reads[LocalDateTime] { json =>
    json.validate[String].flatMap { str =>
      Try {
        if (str.endsWith("Z")) {
          // Handle UTC with Z suffix: "2025-12-25T10:00:00Z"
          LocalDateTime.ofInstant(Instant.parse(str), ZoneOffset.UTC)
        } else if (str.contains("+") || str.contains("-")) {
          // Handle timezone offset: "2025-12-25T15:30:00+05:30"
          LocalDateTime.ofInstant(Instant.parse(str), ZoneOffset.UTC)
        } else {
          // Handle plain format (assume UTC): "2025-12-25T10:00:00"
          LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        }
      }.toOption match {
        case Some(dt) => JsSuccess(dt)
        case None =>
          JsError(
            "Invalid datetime format. Supported formats: " +
              "'2025-12-25T10:00:00Z' (UTC), '2025-12-25T15:30:00+05:30' (with timezone), " +
              "or '2025-12-25T10:00:00' (assumed UTC)"
          )
      }
    }
  }

  implicit val localDateTimeWrites: Writes[LocalDateTime] = new Writes[LocalDateTime] {
    // Always output UTC time with 'Z' suffix to indicate UTC
    def writes(d: LocalDateTime): JsValue = JsString(
      d.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "Z"
    )
  }

  // Standard formatters for the model classes. Play generates these automatically.
  implicit val taskFormat       = Json.format[Task]
  implicit val taskCreateFormat = Json.format[TaskCreate]
  implicit val taskUpdateFormat = Json.format[TaskUpdate]

  def createTask() = authenticatedAction.async(parse.json) { request =>
    val ownerId = request.authContext.userId

    request.body.validate[TaskCreate] match {
      case JsSuccess(task, _) =>
        val sanitizedTask = task.copy(title = sanitiseTitle(task.title))
        validateTaskCreate(sanitizedTask) match {
          case ValidationSuccess =>
            taskService.createTask(sanitizedTask, ownerId).map(id => Created(Json.obj("id" -> id)))

          case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
        }

      case JsError(errors) =>
        val errorDetails = errors.map {
          case (path, validationErrors) =>
            val field = path.toJsonString.replaceAll("^\\.|^/", "")

            val messages = validationErrors.map(_.message).mkString(", ")

            field -> messages
        }.toMap

        Future.successful(BadRequest(Json.obj("errors" -> errorDetails)))
    }
  }

  def updateTask(id: Long) = authenticatedAction.async(parse.json) { request =>
    val ownerId   = request.authContext.userId
    val ownerRole = request.authContext.roles

    request.body.validate[TaskUpdate] match {
      case JsSuccess(taskUpdate, _) =>
        val sanitizedUpdate = taskUpdate.copy(
          title = taskUpdate.title.map(sanitiseTitle)
        )

        validateTaskUpdate(sanitizedUpdate) match {
          case ValidationSuccess =>
            val isAdmin = ownerRole.contains("ADMIN")
            val updateFuture =
              if (isAdmin) taskService.updateAnyTask(sanitizedUpdate, id)
              else taskService.updateTask(sanitizedUpdate, id, ownerId)

            updateFuture.map {
              case Some(updatedTask) => Ok(Json.toJson(updatedTask))
              case None              => NotFound(errorMessage(s"Invalid Task ID: $id"))
            }

          case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
        }

      case JsError(errors) =>
        val errorDetails = errors.map {
          case (path, validationErrors) =>
            val field = path.toJsonString.replaceAll("^\\.|^/", "")

            val messages = validationErrors.map(_.message).mkString(", ")

            field -> messages
        }.toMap

        Future.successful(BadRequest(Json.obj("errors" -> errorDetails)))
    }
  }

  def getTaskByStatus(status: String) = authenticatedAction.async { request =>
    val ownerId   = request.authContext.userId
    val ownerRole = request.authContext.roles

    validateStatus(status) match {
      case ValidationSuccess =>
        val isAdmin = ownerRole.contains("ADMIN")
        val statusFuture =
          if (isAdmin) taskService.getTasksByAnyStatus(status)
          else taskService.getTasksByStatus(status, ownerId)

        statusFuture.map(tasks => Ok(Json.toJson(tasks)))
      case ValidationFailure(message) => Future.successful(BadRequest(errorMessage(message)))
    }
  }

}
