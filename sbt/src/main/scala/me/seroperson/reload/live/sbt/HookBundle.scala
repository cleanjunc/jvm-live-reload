package me.seroperson.reload.live.sbt

sealed trait HookBundle {
  def startupHooks: Seq[String]
  def shutdownHooks: Seq[String]
}

case object ZioAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.ZioAppStartup,
    LiveKeys.HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.ZioAppShutdown,
    LiveKeys.HookClassnames.RuntimeShutdown,
    LiveKeys.HookClassnames.RestApiHealthCheckShutdown
  )
}

case object IoAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.IoAppStartup,
    LiveKeys.HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.IoAppShutdown,
    LiveKeys.HookClassnames.RuntimeShutdown,
    LiveKeys.HookClassnames.RestApiHealthCheckShutdown
  )
}

case object CaskAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.ThreadInterruptShutdown,
    LiveKeys.HookClassnames.RuntimeShutdown,
    LiveKeys.HookClassnames.RestApiHealthCheckShutdown
  )
}

case object SpringBootAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.RestApiHealthCheckStartup
  )
  // No RuntimeShutdownHook: running Spring's JVM shutdown hook flips its `inProgress`
  // flag for good and breaks the next SpringApplication.run().
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.SpringBootAppShutdown,
    LiveKeys.HookClassnames.RestApiHealthCheckShutdown
  )
}

case object GrpcAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.GrpcHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.ThreadInterruptShutdown,
    LiveKeys.HookClassnames.RuntimeShutdown,
    LiveKeys.HookClassnames.GrpcHealthCheckShutdown
  )
}
