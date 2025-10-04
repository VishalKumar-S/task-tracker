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
case class Roles(
  id: Long,
  name: String,
  description: String,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime
)
