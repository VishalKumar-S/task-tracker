package notifications.repositories

import notifications.models._
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}





class NotificationRepository(db: Database)(implicit ec: ExecutionContext) {
  val notifications = NotificationTable.notifications

  def create(notification: Notification): Future[Notification] = db.run((notifications returning notifications.map(_.id) into { case (notif, id)=> notif.copy(id = Some(id))})+=notification)

}
