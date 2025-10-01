package notifications.repositories

import notifications.models._
import slick.jdbc.JdbcProfile
import scala.concurrent.{ExecutionContext, Future}

class NotificationRepository(val profile: JdbcProfile, db: JdbcProfile#Backend#Database)(implicit
  ec: ExecutionContext
) {
  import profile.api._

  val notifications = NotificationTable.notifications

  def create(notification: Notification): Future[Notification] =
    db.run((notifications returning notifications.map(_.id) into {
      case (notif, id) => notif.copy(id = Some(id))
    }) += notification)

}
