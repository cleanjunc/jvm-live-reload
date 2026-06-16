package me.seroperson.reload.live.gradle

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test

@Timeout(value = 5, unit = TimeUnit.MINUTES)
class LiveReloadJoobyTest : LiveReloadTestBase() {
    @field:TempDir lateinit var projectDir: File

    private val appCode by lazy {
        val kotlinSources = projectDir.resolve("src/main/kotlin")
        kotlinSources.mkdirs()
        kotlinSources.resolve("App.kt")
    }
    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun `reload jooby`() {
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
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    application
    id("me.seroperson.reload.live.gradle")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.jooby:jooby-kotlin:3.6.1")
    implementation("io.jooby:jooby-netty:3.6.1")
}

liveReload { settings = mapOf("live.reload.http.port" to "8081") }

application { mainClass = "AppKt" }
"""
        const val APP_CODE_1 =
            """
import io.jooby.StatusCode
import io.jooby.kt.Kooby

fun main() {
    val app = Kooby {
        serverOptions { port = 8081 }
        get("/greet") { "Hello World" }
        get("/health") { ctx.send(StatusCode.OK) }
    }
    try {
        app.start()
        Thread.currentThread().join()
    } catch (ex: InterruptedException) {
        app.stop()
    }
}
"""
        const val APP_CODE_2 =
            """
import io.jooby.StatusCode
import io.jooby.kt.Kooby

fun main() {
    val app = Kooby {
        serverOptions { port = 8081 }
        get("/greet_reloaded") { "World Hello" }
        get("/health") { ctx.send(StatusCode.OK) }
    }
    try {
        app.start()
        Thread.currentThread().join()
    } catch (ex: InterruptedException) {
        app.stop()
    }
}
"""
    }
}
