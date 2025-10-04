package utils

import actions.{AuthenticatedAction, AuthenticatedRequest, AuthContext}
import models.User
import play.api.mvc._

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

/**
 * A utility for mocking authentication in controller tests.
 */
object TestAuth {
  // A standard test user to use across all tests
  val testUser: User =
    User(1L, "testuser", "test@example.com", "hashed-password", LocalDateTime.now(), LocalDateTime.now())

  /**
   * Creates a fake AuthenticatedAction for testing that bypasses real authentication. This returns a subclass that always authenticates as testUser.
   * @param cc The controller components needed by the action.
   * @param ec The execution context.
   * @return A mocked AuthenticatedAction.
   */
  def successful(cc: ControllerComponents)(implicit ec: ExecutionContext): AuthenticatedAction =
    new AuthenticatedAction(new BodyParsers.Default(cc.parsers), null, null)(ec) {

      val testUserAuthContext = new AuthContext(testUser.id, Set("USER"))

      override def invokeBlock[A](
        request: Request[A],
        block: AuthenticatedRequest[A] => Future[Result]
      ): Future[Result] =
        block(new AuthenticatedRequest(testUserAuthContext, request))
    }
}
