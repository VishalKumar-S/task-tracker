package repositories

import models.{RefreshToken, RefreshTokenTableDef}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.time.{LocalDateTime, ZoneOffset}
import com.typesafe.config.ConfigFactory

/**
 * A repository for managing RefreshToken data.
 * This class provides a clean API for database operations on the `refresh_tokens` table.
 *
 * @param dbConfigProvider The provider for the database configuration.
 * @param ec The execution context for asynchronous operations.
 */

@Singleton
class RefreshTokenRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit
  ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val refreshTokens = RefreshTokenTableDef.refreshTokens

  def findByToken(token: String): Future[Option[RefreshToken]] =
    db.run(refreshTokens.filter(_.tokenHash === token).result.headOption)

  def upsertByUserId(userId: Long, tokenHash: String, expiresAt: LocalDateTime): Future[Int] = {
    val now = LocalDateTime.now(ZoneOffset.UTC)

    val upsertAction = for {
      existingTokenOpt <- refreshTokens.filter(_.userId === userId).result.headOption
      result <- existingTokenOpt match {
                  case Some(refreshToken) =>
                    refreshTokens
                      .filter(_.userId === userId)
                      .map(rt => (rt.tokenHash, rt.expiresAt, rt.updatedAt))
                      .update((tokenHash, expiresAt, now))

                  case None =>
                    refreshTokens += RefreshToken(
                      id = None,
                      userId = userId,
                      tokenHash = tokenHash,
                      expiresAt = expiresAt,
                      createdAt = now,
                      updatedAt = now,
                      isRevoked = false
                    )

                }
    } yield result

    db.run(upsertAction.transactionally)

  }

}
