package jobs

import org.apache.pekko.actor.ActorSystem
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import services.TaskService
import play.api.Configuration


@Singleton
class TaskScheduler @Inject()(taskService: TaskService, actorSystem: ActorSystem, configuration: Configuration)(implicit ec: ExecutionContext) {

  private val initialDelay: FiniteDuration = configuration.get[FiniteDuration]("jobs.TaskScheduler.initialDelay")
  private val interval: FiniteDuration = configuration.get[FiniteDuration]("jobs.TaskScheduler.interval")

  actorSystem.scheduler.scheduleAtFixedRate(
    initialDelay = initialDelay,
    interval = interval
  ) {
    () =>
      taskService.processDueTasks()
  }
}

