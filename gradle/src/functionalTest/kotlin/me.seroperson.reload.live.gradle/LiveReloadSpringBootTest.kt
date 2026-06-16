package me.seroperson.reload.live.gradle

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class LiveReloadSpringBootTest : LiveReloadTestBase() {
    @field:TempDir lateinit var projectDir: File

    private val appCode by lazy {
        val kotlinSources = projectDir.resolve("src/main/kotlin/app")
        kotlinSources.mkdirs()
        kotlinSources.resolve("App.kt")
    }
    private val appProperties by lazy {
        val resources = projectDir.resolve("src/main/resources")
        resources.mkdirs()
        resources.resolve("application.properties")
    }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `reload spring boot`() {
        settingsFile.writeText(SETTINGS_CONTENT)
        buildFile.writeText(BUILD_CONTENT)
        appProperties.writeText(APP_PROPERTIES)
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
        const val APP_PROPERTIES =
            """
server.port=8081
spring.main.banner-mode=off
"""
        const val BUILD_CONTENT =
            """
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.0"
    application
    id("me.seroperson.reload.live.gradle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:3.3.5")
}

liveReload {
    settings = mapOf("live.reload.http.port" to "8081")
    shutdownHooks = listOf(
        "me.seroperson.reload.live.hook.spring.SpringBootAppShutdownHook",
        "me.seroperson.reload.live.hook.RestApiHealthCheckShutdownHook",
    )
}

application { mainClass = "app.AppKt" }
"""
        const val APP_CODE_1 =
            """
package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class App {
    @GetMapping("/greet")
    fun greet(): String = "Hello World"

    @GetMapping("/health")
    fun health(): String = "OK"
}

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
"""
        const val APP_CODE_2 =
            """
package app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class App {
    @GetMapping("/greet_reloaded")
    fun greetReloaded(): String = "World Hello"

    @GetMapping("/health")
    fun health(): String = "OK"
}

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
"""
    }
}
