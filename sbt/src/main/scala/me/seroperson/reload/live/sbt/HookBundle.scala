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

case object MicronautAppHookBundle extends HookBundle {
  def startupHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.RestApiHealthCheckStartup
  )
  def shutdownHooks: Seq[String] = Seq(
    LiveKeys.HookClassnames.MicronautAppShutdown,
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
