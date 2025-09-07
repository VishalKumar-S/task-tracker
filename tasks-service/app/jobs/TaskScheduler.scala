package jobs

import org.apache.pekko.actor.ActorSystem
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import services.TaskService


@Singleton
class TaskScheduler @Inject()(taskService: TaskService, actorSystem: ActorSystem)(implicit ec: ExecutionContext) {


  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = 0.seconds,
    interval = 1.minute
  ) {
    () =>
      taskService.processDueTasks()
  }
}

