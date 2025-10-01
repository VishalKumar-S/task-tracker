package controllers
import models.{User, UserCreate, UserLogin}
import services.AuthService
import repositories.UserRepository
import play.api.mvc._
import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._

@Singleton
class AuthController @Inject() (cc: ControllerComponents, authService: AuthService, userRepository: UserRepository)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  // --- JSON Serialization ---
  implicit private val userCreateReads = Json.reads[UserCreate]
  implicit private val userLoginReads  = Json.reads[UserLogin]

  private case class LoginResponse(token: String)

  implicit private val loginResponseWrites: Writes[LoginResponse] = Json.writes[LoginResponse]

  def register(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body
      .validate[UserCreate]
      .fold(
        errors =>
          Future.successful(
            BadRequest(
              Json.obj("message" -> "Invalid JSON format for registration.", "details" -> JsError.toJson(errors))
            )
          ),
        userCreate =>
          userRepository.create(userCreate, authService.hashPassword(userCreate.password)).map { user =>
            Created(Json.obj("message" -> s"User '${user.username}' created successfully."))
          }
      )
  }

  def login(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body
      .validate[UserLogin]
      .fold(
        errors =>
          Future.successful(
            BadRequest(Json.obj("message" -> "Invalid JSON format for login.", "details" -> JsError.toJson(errors)))
          ),
        userLogin =>
          userRepository.findByUsername(userLogin.username).map {
            case Some(existingUser) if authService.checkPassword(userLogin.password, existingUser.passwordHash) =>
              val token = authService.generateToken(existingUser)
              Ok(Json.toJson(LoginResponse(token)))
            case _ => Unauthorized(Json.obj("message" -> "Invalid username or password."))
          }
      )
  }

}
