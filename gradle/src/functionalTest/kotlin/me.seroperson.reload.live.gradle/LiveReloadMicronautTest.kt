package me.seroperson.reload.live.gradle

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class LiveReloadMicronautTest : LiveReloadTestBase() {
    @field:TempDir lateinit var projectDir: File

    private val appCode by lazy {
        val javaSources = projectDir.resolve("src/main/java/com/example")
        javaSources.mkdirs()
        javaSources.resolve("Application.java")
    }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `reload micronaut`() {
        settingsFile.writeText(SETTINGS_CONTENT)
        buildFile.writeText(BUILD_CONTENT)
        appCode.writeText(APP_CODE_1)

        val runner = initGradleRunner(":liveReloadRun", projectDir)
        val isBuildRunning = AtomicBoolean(true)
        val runThread =
            Thread {
                try {
                    runner.build()
                    isBuildRunning.set(false)
                } catch (_: InterruptedException) {
                    println("Interrupted")
                } catch (ex: Exception) {
                    println("Got exception ${ex.message}")
                }
            }
        runThread.start()

        val greet = runUntil(isBuildRunning, "http://localhost:9000/greet", 200, "Hello World")

        appCode.writeText(APP_CODE_2)

        val greetReloaded =
            runUntil(isBuildRunning, "http://localhost:9000/greet_reloaded", 200, "World Hello")

        runThread.interrupt()

        assertTrue(greet && greetReloaded)
    }

    companion object {
        const val SETTINGS_CONTENT = ""
        const val BUILD_CONTENT =
            """
plugins {
    java
    application
    id("me.seroperson.reload.live.gradle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    annotationProcessor(platform("io.micronaut.platform:micronaut-platform:4.7.6"))
    annotationProcessor("io.micronaut:micronaut-inject-java")
    implementation(platform("io.micronaut.platform:micronaut-platform:4.7.6"))
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    runtimeOnly("ch.qos.logback:logback-classic")
}

liveReload {
    settings =
        mapOf(
            "live.reload.http.port" to "8081",
        )
    shutdownHooks.set(
        listOf(
            "me.seroperson.reload.live.hook.MicronautAppShutdownHook",
            "me.seroperson.reload.live.hook.RestApiHealthCheckShutdownHook",
        ),
    )
}

application { mainClass = "com.example.Application" }
"""
        const val APP_CODE_1 =
            """
package com.example;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(String[] args) {
        System.setProperty("micronaut.server.port", "8081");
        // Scan the reloaded class loader for beans, not Micronaut's own.
        Micronaut.build(args)
            .classLoader(Application.class.getClassLoader())
            .mainClass(Application.class)
            .start();
    }
}

@Controller
class GreetController {

    @Get("/greet")
    @Produces(MediaType.TEXT_PLAIN)
    public String greet() {
        return "Hello World";
    }

    @Get("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }
}
"""
        const val APP_CODE_2 =
            """
package com.example;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;
import io.micronaut.runtime.Micronaut;

public class Application {
    public static void main(String[] args) {
        System.setProperty("micronaut.server.port", "8081");
        // Scan the reloaded class loader for beans, not Micronaut's own.
        Micronaut.build(args)
            .classLoader(Application.class.getClassLoader())
            .mainClass(Application.class)
            .start();
    }
}

@Controller
class GreetController {

    @Get("/greet_reloaded")
    @Produces(MediaType.TEXT_PLAIN)
    public String greet() {
        return "World Hello";
    }

    @Get("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }
}
"""
    }
}
