package me.seroperson.reload.live.gradle

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

abstract class LiveReloadTestBase {
    private val client = OkHttpClient()

    fun initGradleRunner(
        command: String,
        projectDir: File,
    ): GradleRunner {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withGradleVersion("8.14.3")
        runner.withPluginClasspath()
        runner.withProjectDir(projectDir)
        runner.withEnvironment(mapOf("GRADLE_OPTS" to "--add-opens=java.base/java.nio=ALL-UNNAMED"))
        runner.withArguments(
            command,
            "--info",
            "--watch-fs",
            "--stacktrace",
            "-Dorg.gradle.vfs.verbose=true",
            "-Dorg.gradle.native=true",
        )
        return runner
    }

    fun runUntil(
        isBuildRunning: AtomicBoolean,
        url: String,
        expectedStatus: Int,
        expectedBody: String,
    ): Boolean {
        if (!isBuildRunning.get()) {
            return false
        }
        val request: Request = Request.Builder().url(url).build()

        try {
            val (code, body) =
                (
                    client.newCall(request).execute().use { response ->
                        response.code to response.body.string()
                    }
                )
            println("Requesting $url, got $code and $body")
            if (expectedStatus == code && expectedBody == body) {
                return true
            } else {
                Thread.sleep(500)
                return runUntil(isBuildRunning, url, expectedStatus, expectedBody)
            }
        } catch (ex: Exception) {
            println("Got exception: ${ex.message}")
            Thread.sleep(500)
            return runUntil(isBuildRunning, url, expectedStatus, expectedBody)
        }
    }
}
