package me.seroperson.reload.live.hook.spring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.seroperson.reload.live.build.BuildLogger;
import me.seroperson.reload.live.hook.Hook;
import me.seroperson.reload.live.reflect.MiscUtils;
import me.seroperson.reload.live.settings.DevServerSettings;

/**
 * Closes the running Spring Boot {@code ApplicationContext}(s) on reload.
 *
 * <p>A Spring Boot {@code main()} returns immediately while the embedded server keeps running on its
 * own non-daemon threads, so interrupting the main thread can't stop it. This hook discovers the
 * live contexts through Spring's static {@code SpringApplicationShutdownHook} and closes them, which
 * stops the embedded server and joins its threads. All reflection targets the reloaded classloader,
 * where Spring lives.
 */
public class SpringBootAppShutdownHook implements Hook {

  @Override
  public String description() {
    return "Closes running Spring Boot ApplicationContext(s)";
  }

  @Override
  public boolean isAvailable() {
    return MiscUtils.hasClass("org.springframework.boot.SpringApplication");
  }

  @Override
  public void hook(Thread th, ClassLoader cl, DevServerSettings settings, BuildLogger logger) {
    List<Object> contexts;
    try {
      contexts = runningContexts(cl);
    } catch (ReflectiveOperationException | RuntimeException ex) {
      logger.warn("Unable to locate Spring Boot ApplicationContext(s): " + ex.getMessage());
      return;
    }

    if (contexts.isEmpty()) {
      logger.debug("No running Spring Boot ApplicationContext found");
      return;
    }

    for (Object ctx : contexts) {
      try {
        logger.debug("Closing Spring Boot ApplicationContext " + ctx);
        ctx.getClass().getMethod("close").invoke(ctx);
      } catch (ReflectiveOperationException ex) {
        logger.error("Failed to close Spring Boot ApplicationContext " + ctx, ex);
      }
    }

    awaitThreadShutdown(th, settings.getThreadInterruptTimeoutMs(), logger);
  }

  /** Snapshot of the contexts tracked by Spring's static shutdown hook. */
  private List<Object> runningContexts(ClassLoader cl) throws ReflectiveOperationException {
    Class<?> springApplication =
        Class.forName("org.springframework.boot.SpringApplication", false, cl);

    Field hookField = springApplication.getDeclaredField("shutdownHook");
    hookField.setAccessible(true);
    Object shutdownHook = hookField.get(null);
    if (shutdownHook == null) {
      return List.of();
    }

    Field contextsField = shutdownHook.getClass().getDeclaredField("contexts");
    contextsField.setAccessible(true);
    if (!(contextsField.get(shutdownHook) instanceof Set<?> contexts)) {
      return List.of();
    }
    // Copy: closing a context removes it from the live set.
    return new ArrayList<>(contexts);
  }

  /** Bounded wait for the embedded server's non-daemon threads to finish; warns on leftovers. */
  private void awaitThreadShutdown(Thread th, long timeoutMs, BuildLogger logger) {
    ThreadGroup group = th.getThreadGroup();
    if (group == null) {
      return;
    }

    long deadline = System.currentTimeMillis() + timeoutMs;
    Thread[] threads = new Thread[group.activeCount() + 8];
    int count = group.enumerate(threads);

    List<Thread> leaked = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Thread t = threads[i];
      if (t == Thread.currentThread() || t.isDaemon()) {
        continue;
      }
      long remaining = deadline - System.currentTimeMillis();
      if (remaining > 0) {
        try {
          t.join(remaining);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return;
        }
      }
      if (t.isAlive()) {
        leaked.add(t);
      }
    }

    if (!leaked.isEmpty()) {
      logger.warn("Spring Boot shutdown left non-daemon threads alive after " + timeoutMs + "ms: "
          + leaked);
    }
  }
}
