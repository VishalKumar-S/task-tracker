package actions

import javax.inject.Inject
import models.User
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc._
import repositories.UserRepository
import services.AuthService


import scala.concurrent.{ExecutionContext, Future}

/**
 * A custom action builder that handles authentication.
 * It checks for a valid JWT in the request headers and, if present,
 * fetches the corresponding user, wrapping the request in an `AuthenticatedRequest`.
 * If the token is missing or invalid, it returns a 401 Unauthorized result.
 *
 * @param bodyParser The standard Play body parser.
 * @param authService The service for validating JWTs.
 * @param userRepo The repository for fetching user data.
 * @param ec The execution context for asynchronous operations.
 */


class AuthenticatedAction @Inject()(bodyParser: BodyParsers.Default, authService: AuthService, userRepo: UserRepository)(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest, AnyContent]{
  override def parser: BodyParser[AnyContent] = bodyParser
  override protected def executionContext: ExecutionContext = ec

  private val bearerTokenRegex = "Bearer (.+)".r

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val maybeToken = request.headers.get(HeaderNames.AUTHORIZATION).flatMap {
      case bearerTokenRegex(token) => Some(token)
      case _ => None
    }


    maybeToken match {
      case Some(token) =>
        authService.validateToken(token) match {
          case Some(userId) =>
            userRepo.findById(userId).flatMap {
              case Some(user) => block(new AuthenticatedRequest(user, request))
              case None => Future.successful(Results.Unauthorized(Json.obj("message" -> "Invalid credentials.")))
            }
          case None => Future.successful(Results.Unauthorized(Json.obj("message" -> "Invalid or expired token.")))
        }

      case None =>
        Future.successful(Results.Unauthorized(Json.obj("message" -> "Authentication token not provided.")))
    }

  }
}




