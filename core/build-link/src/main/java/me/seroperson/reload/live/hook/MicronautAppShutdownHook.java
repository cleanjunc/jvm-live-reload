package me.seroperson.reload.live.hook;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import me.seroperson.reload.live.UnrecoverableException;
import me.seroperson.reload.live.build.BuildLogger;
import me.seroperson.reload.live.reflect.MiscUtils;
import me.seroperson.reload.live.reflect.ShutdownHook;
import me.seroperson.reload.live.settings.DevServerSettings;

/**
 * Shutdown hook for Micronaut applications.
 *
 * <p>{@code Micronaut.run(...)} starts the embedded (Netty) server on its own threads and returns,
 * so the application {@code main} thread is already gone by shutdown time and {@link
 * ThreadInterruptShutdownHook} has nothing to interrupt. Instead, this hook triggers the JVM
 * shutdown hook Micronaut registers to close the {@code ApplicationContext} (i.e. {@code
 * context.stop()}), then waits for the server threads of the current reload generation to finish so
 * none leak across reloads.
 *
 * <p>Those threads are matched by their context {@link ClassLoader}: the runner starts the
 * application under the reloaded class loader and Netty's threads inherit it, which also avoids
 * touching the dev-server's own proxy threads.
 */
public class MicronautAppShutdownHook implements Hook {

  @Override
  public String description() {
    return "Stops a Micronaut ApplicationContext and joins its server threads";
  }

  @Override
  public boolean isAvailable() {
    return MiscUtils.hasClass("io.micronaut.context.ApplicationContext");
  }

  @Override
  public void hook(Thread th, ClassLoader cl, DevServerSettings settings, BuildLogger logger) {
    // Run Micronaut's JVM shutdown hook to stop the context, then reset the hooks so the next
    // reload generation starts clean.
    ShutdownHook.runApplicationShutdownHooks(logger);
    ShutdownHook.setShutdownHooks(new IdentityHashMap<>());

    long timeoutMs = settings.getThreadInterruptTimeoutMs();
    long deadline = System.currentTimeMillis() + timeoutMs;
    logger.debug("Waiting up to " + timeoutMs + "ms for Micronaut server threads to finish");

    List<Thread> serverThreads = applicationThreads(cl);
    for (Thread t : serverThreads) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        break;
      }
      try {
        t.join(remaining);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        logger.error("Interrupted while waiting for Micronaut server threads", ex);
        return;
      }
    }

    var leaked = serverThreads.stream().filter(Thread::isAlive).map(Thread::getName).toList();
    if (!leaked.isEmpty()) {
      throw new UnrecoverableException(
          "Micronaut server threads did not terminate within "
              + timeoutMs
              + "ms after the ApplicationContext was stopped: "
              + leaked
              + ". Configure '"
              + DevServerSettings.LiveReloadThreadInterruptTimeout
              + "' to adjust the timeout.");
    }
  }

  /** Live threads of the current reload generation, identified by the reloaded class loader. */
  private static List<Thread> applicationThreads(ClassLoader cl) {
    ThreadGroup root = Thread.currentThread().getThreadGroup();
    while (root.getParent() != null) {
      root = root.getParent();
    }

    Thread[] threads;
    int count;
    int size = Math.max(root.activeCount() * 2, 64);
    do {
      threads = new Thread[size];
      count = root.enumerate(threads, true);
      size *= 2;
    } while (count == threads.length);

    var current = Thread.currentThread();
    var result = new ArrayList<Thread>();
    for (int i = 0; i < count; i++) {
      Thread t = threads[i];
      if (t != current && t.isAlive() && t.getContextClassLoader() == cl) {
        result.add(t);
      }
    }
    return result;
  }
}
