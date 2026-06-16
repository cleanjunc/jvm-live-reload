package me.seroperson.reload.live.hook.pekko

import me.seroperson.reload.live.UnrecoverableException
import me.seroperson.reload.live.build.BuildLogger
import me.seroperson.reload.live.hook.Hook
import me.seroperson.reload.live.reflect.MiscUtils
import me.seroperson.reload.live.settings.DevServerSettings

/** Shutdown hook for Apache Pekko HTTP applications.
  *
  * Interrupts the application thread and waits for the `ActorSystem` dispatcher
  * threads to exit. The application `main` is expected to catch the
  * `InterruptedException` and call `system.terminate()`; this hook only ensures
  * no Pekko threads leak across a reload.
  */
class PekkoHttpAppShutdownHook extends Hook {

  override def description: String =
    "Shutdown an Apache Pekko HTTP application"

  override def isAvailable: Boolean =
    MiscUtils.hasClass("org.apache.pekko.actor.ActorSystem")

  override def hook(
      th: Thread,
      cl: ClassLoader,
      settings: DevServerSettings,
      logger: BuildLogger
  ): Unit = {
    val timeoutMs: Long = settings.getThreadInterruptTimeoutMs()

    def awaitExit(thread: Thread): Unit = {
      thread.join(timeoutMs)
      if (thread.isAlive) {
        throw new UnrecoverableException(
          s"Pekko thread '${thread.getName}' did not exit within ${timeoutMs}ms. " +
            s"Make sure your main calls system.terminate() on InterruptedException, " +
            s"or raise '${DevServerSettings.LiveReloadThreadInterruptTimeout}'."
        )
      }
    }

    th.interrupt()
    logger.debug(s"Waiting up to ${timeoutMs}ms for Pekko threads to finish")
    awaitExit(th)

    // The app thread only returns once terminate() completes, so any remaining
    // dispatcher threads should already be winding down; join them to be sure.
    val appThreadGroup = th.getThreadGroup
    if (appThreadGroup != null) {
      val threads = new Array[Thread](appThreadGroup.activeCount())
      val count = appThreadGroup.enumerate(threads)
      threads
        .take(count)
        .filter(t => t != null && t.getName.contains("pekko.actor"))
        .foreach(awaitExit)
    }
  }

}
