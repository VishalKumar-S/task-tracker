package jobs

import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment, Logger}

class JobsModule extends Module {
  private val logger = Logger(this.getClass)

  override def bindings(env: Environment, conf: Configuration): Seq[Binding[_]] = {
    logger.info(">>> JobsModule bindings loaded at startup")
    Seq(
      // Bind the TaskScheduler to be eagerly instantiated at application startup.
      // The `.eagerly()` call is critical here; without it, the scheduler would only be
      // created when first requested, meaning the background job would not start automatically.
      bind[TaskScheduler].toSelf.eagerly()
    )
  }
}