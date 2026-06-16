package me.seroperson.reload.live.mill

sealed trait HookBundle {
  def startupHooks: Seq[String]
  def shutdownHooks: Seq[String]
}

case object ZioAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    HookClassnames.ZioAppStartup,
    HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    HookClassnames.ZioAppShutdown,
    HookClassnames.RuntimeShutdown,
    HookClassnames.RestApiHealthCheckShutdown
  )
}

case object IoAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    HookClassnames.IoAppStartup,
    HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    HookClassnames.IoAppShutdown,
    HookClassnames.RuntimeShutdown,
    HookClassnames.RestApiHealthCheckShutdown
  )
}

case object CaskAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    HookClassnames.ThreadInterruptShutdown,
    HookClassnames.RuntimeShutdown,
    HookClassnames.RestApiHealthCheckShutdown
  )
}

case object SpringBootAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    HookClassnames.RestApiHealthCheckStartup
  )
  // No RuntimeShutdownHook: running Spring's JVM shutdown hook flips its `inProgress`
  // flag for good and breaks the next SpringApplication.run().
  def shutdownHooks: Seq[String] = Seq(
    HookClassnames.SpringBootAppShutdown,
    HookClassnames.RestApiHealthCheckShutdown
  )
}

case object GrpcAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    HookClassnames.GrpcHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    HookClassnames.ThreadInterruptShutdown,
    HookClassnames.RuntimeShutdown,
    HookClassnames.GrpcHealthCheckShutdown
  )
}
