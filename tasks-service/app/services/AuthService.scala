package services

import javax.inject.{Inject, Singleton}
import models.User
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{JwtAlgorithm, JwtClaim, Jwt}
import play.api.Configuration
import scala.util.Try
import java.time.Clock

/**
 * A service to handle core authentication logic like password hashing and JWT management.
 * This service does NOT interact with the database. It only performs cryptographic operations.
 *
 * @param config The application configuration, used to get the JWT secret key and expiration.
 */
@Singleton
class AuthService @Inject() (config: Configuration) {

  // --- Configuration and Setup ---

  private val secretKey = config.get[String]("play.http.secret.key")

  // HMAC SHA-256 is a strong, standard algorithm used to sign the JWT.
  private val algorithm = JwtAlgorithm.HS256

  private val expirationInSeconds = config.get[Long]("jwt.expiration")

  implicit val clock: Clock = Clock.systemUTC()

  // --- Password Management ---

  /**
   * Hashes a plain-text password using the BCrypt algorithm.
   * BCrypt is an industry-standard because it's slow and includes a "salt" to protect against common attacks.
   *
   * @param password The plain-text password to hash.
   * @return A securely hashed password string (e.g., "$2a$10$...") that you can safely store in the database.
   */
  def hashPassword(password: String): String =
    BCrypt.hashpw(password, BCrypt.gensalt())

  /**
   * Checks if a plain-text password from a login attempt matches a stored BCrypt hash.
   *
   * @param candidate The plain-text password from the user's login form.
   * @param hash The stored password hash from the `users` table.
   * @return true if the password is correct, false otherwise.
   */
  def checkPassword(candidate: String, hash: String): Boolean =
    BCrypt.checkpw(candidate, hash)

  // --- JWT (Token) Management ---

  /**
   * Generates a new JWT for a given user after they successfully log in.
   * The token "claims" (contains) the user's ID, which we can use to identify them in future requests.
   *
   * @param user The user for whom to generate the token.
   * @return A signed JWT string. This is what you send back to the client.
   */
  def generateToken(user: User): String = {
    val claim = JwtClaim(
      subject = Some(user.id.toString)
    ).issuedNow.expiresIn(expirationInSeconds)

    Jwt.encode(claim, secretKey, algorithm)
  }

  /**
   * Validates a JWT string received from a client.
   * It checks the signature and expiration date.
   *
   * @param token The JWT string from the `Authorization` header.
   * @return The user ID (as a Long) if the token is valid, or None if it's invalid or expired.
   */
  def validateToken(token: String): Option[Long] =
    Jwt
      .decode(token, secretKey, Seq(algorithm))
      .toOption
      .flatMap(_.subject)
      .flatMap(idString => Try(idString.toLong).toOption)
}
