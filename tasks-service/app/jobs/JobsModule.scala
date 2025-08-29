package jobs

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment, Logger}

class JobsModule extends Module {
  private val logger = Logger(this.getClass)

  override def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = {
    logger.info(">>> JobsModule bindings loaded at startup")
    Seq(
      bind[TaskScheduler].toSelf.eagerly()
    )
  }
}