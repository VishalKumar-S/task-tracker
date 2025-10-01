package models

import java.time.LocalDateTime

/**
 * Represents a full User entity as it is stored in the database.
 *
 * @param id The unique identifier for the user.
 * @param username The user's unique username.
 * @param email The user's unique email address.
 * @param passwordHash The securely hashed password.
 * @param createdAt The timestamp when the user was created.
 * @param updatedAt The timestamp when the user was last updated.
 */
case class User(
  id: Long,
  username: String,
  email: String,
  passwordHash: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
)

/** DTO for a user registration request. */
case class UserCreate(username: String, email: String, password: String)

/** DTO for a user login request. */
case class UserLogin(username: String, password: String)
