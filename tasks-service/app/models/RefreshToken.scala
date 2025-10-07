package models

import java.time.LocalDateTime

/**
 * Represents a refresh token in the database.
 *
 * @param id The unique identifier for the refresh token.
 * @param userId The ID of the user this token belongs to.
 * @param tokenHash A secure hash of the refresh token string.
 * @param expiresAt The timestamp when this token will expire.
 * @param createdAt The timestamp when this token was created.
 * @param updatedAt The timestamp when this token was last updated.
 * @param isRevoked A flag to indicate if the token has been manually revoked (e.g., on logout).
 */
case class RefreshToken(
  id: Option[Long],
  userId: Long,
  tokenHash: String,
  expiresAt: LocalDateTime,
  createdAt: LocalDateTime,
  updatedAt: LocalDateTime,
  isRevoked: Boolean
)

/**
 * DTO for refresh token requests
 */
case class RefreshTokenRequest(refreshToken: String)
