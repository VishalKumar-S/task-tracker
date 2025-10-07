package models

import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp
import java.time.{LocalDateTime, ZoneOffset}

/**
 * Slick table definition for the 'refresh_tokens' table.
 *
 * This table stores refresh tokens for user authentication with the following features:
 * - Token rotation support
 * - Revocation capability for security
 * - Expiration tracking
 * - Foreign key relationship with users table
 */
class RefreshTokenTableDef(tag: Tag) extends Table[RefreshToken](tag, "refresh_tokens") {

  // Reuse the same LocalDateTime mapping from UserTableDef
  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] = MappedColumnType.base[LocalDateTime, Timestamp](
    ldt => Timestamp.from(ldt.toInstant(ZoneOffset.UTC)),
    ts => LocalDateTime.ofInstant(ts.toInstant, ZoneOffset.UTC)
  )

  def id        = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId    = column[Long]("user_id")
  def tokenHash = column[String]("token_hash")
  def expiresAt = column[LocalDateTime]("expires_at")
  def createdAt = column[LocalDateTime]("created_at")
  def updatedAt = column[LocalDateTime]("updated_at")
  def isRevoked = column[Boolean]("is_revoked")

  override def * =
    (id.?, userId, tokenHash, expiresAt, createdAt, updatedAt, isRevoked) <> (RefreshToken.tupled, RefreshToken.unapply)

  // Foreign key to users table
  def user = foreignKey("fk_refresh_token_user", userId, UserTableDef.users)(
    _.id,
    onDelete = ForeignKeyAction.Cascade
  )

  // Indexes (Slick automatically uses these based on the database schema)
  def idxTokenHash = index("idx_token_hash", tokenHash, unique = true)
  def idxUserId    = index("idx_user_id", userId)
}

object RefreshTokenTableDef {

  /**
   * The TableQuery object for the 'refresh_tokens' table.
   * This is the entry point for all database queries involving refresh tokens.
   */
  val refreshTokens = TableQuery[RefreshTokenTableDef]
}
