package actions

import models.User
import play.api.mvc.{Request, WrappedRequest}

/**
 * A custom request wrapper that includes the authenticated user's information.
 *
 * By extending `WrappedRequest`, we can pass this around just like a normal `Request`,
 * but with the added `user` field. This makes accessing the logged-in user in
 * controller actions clean and type-safe.
 *
 * @param user The authenticated `User` object, fetched from the database.
 * @param request The original `Request` that this wrapper is augmenting.
 * @tparam A The body type of the request.
 */


class AuthenticatedRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)