package controllers
import models.{User, UserCreate, UserLogin, RefreshTokenRequest}
import services.AuthService
import repositories.{UserRepository, RefreshTokenRepository}
import play.api.mvc._
import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import java.time.{LocalDateTime, ZoneOffset}

@Singleton
class AuthController @Inject() (
  cc: ControllerComponents,
  authService: AuthService,
  userRepository: UserRepository,
  refreshTokenRepository: RefreshTokenRepository
)(implicit
  ec: ExecutionContext
) extends AbstractController(cc) {

  // --- JSON Serialization ---

  implicit private val refreshTokenReqReads = Json.reads[RefreshTokenRequest]

  implicit private val userCreateReads = Json.reads[UserCreate]
  implicit private val userLoginReads  = Json.reads[UserLogin]

  private case class LoginResponse(accessToken: String, refreshToken: String)
  implicit private val loginResponseWrites: Writes[LoginResponse] = Json.writes[LoginResponse]

  private case class RefreshResponse(accessToken: String, refreshToken: String)
  implicit private val refreshResponseWrites: Writes[RefreshResponse] = Json.writes[RefreshResponse]

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
          for {
            user <- userRepository.create(userCreate, authService.hashPassword(userCreate.password))
            _    <- userRepository.assignUserRole(user.id, "USER")
          } yield Created(Json.obj("message" -> s"User '${user.username}' created successfully."))
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
          userRepository.findByUsername(userLogin.username).flatMap {
            case Some(existingUser) if authService.checkPassword(userLogin.password, existingUser.passwordHash) =>
              userRepository.findUserRole(existingUser.id).flatMap { roles =>
                val accessToken = authService.generateToken(existingUser, roles)

                val refreshToken     = authService.generateRefreshToken()
                val refreshTokenHash = authService.hashToken(refreshToken)
                val expiresAt        = authService.getRefreshTokenExpiry()

                refreshTokenRepository
                  .upsertByUserId(existingUser.id, refreshTokenHash, expiresAt)
                  .map(_ => Ok(Json.toJson(LoginResponse(accessToken, refreshToken))))
              }
            case _ => Future.successful(Unauthorized(Json.obj("message" -> "Invalid username or password.")))
          }
      )
  }

  /**
   * Refresh access token using refresh token.
   * Implements token rotation - issues new refresh token on each refresh.
   * POST /refresh
   */
  def refresh(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body
      .validate[RefreshTokenRequest]
      .fold(
        errors =>
          Future.successful(
            BadRequest(Json.obj("message" -> "Invalid JSON format for refresh.", "details" -> JsError.toJson(errors)))
          ),
        refreshTokenRequest => {
          val plainToken  = refreshTokenRequest.refreshToken
          val hashedToken = authService.hashToken(plainToken)

          refreshTokenRepository.findByToken(hashedToken).flatMap {
            case Some(refreshToken)
                if !refreshToken.isRevoked && refreshToken.expiresAt.isAfter(LocalDateTime.now(ZoneOffset.UTC)) =>
              userRepository.findById(refreshToken.userId).flatMap {
                case Some(user) =>
                  userRepository.findUserRole(user.id).flatMap { roles =>
                    val newAccessToken      = authService.generateToken(user, roles)
                    val newRefreshToken     = authService.generateRefreshToken()
                    val newRefreshTokenHash = authService.hashToken(newRefreshToken)
                    val expiresAt           = authService.getRefreshTokenExpiry()

                    refreshTokenRepository.upsertByUserId(refreshToken.userId, newRefreshTokenHash, expiresAt).map {
                      _ => Ok(Json.toJson(RefreshResponse(newAccessToken, newRefreshToken)))
                    }
                  }
                case None =>
                  Future.successful(Unauthorized(Json.obj("message" -> "User not found.")))
              }

            case Some(_) =>
              Future.successful(Unauthorized(Json.obj("message" -> "Refresh token expired or revoked.")))
            case None =>
              Future.successful(Unauthorized(Json.obj("message" -> "Invalid refresh token.")))
          }
        }
      )
  }

}
