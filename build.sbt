lazy val scala212 = "2.12.21"
lazy val scala213 = "2.13.18"
lazy val scala3 = "3.8.2"
lazy val supportedScalaVersions = List(scala212, scala213, scala3)
lazy val supportedScalaSbtVersions = List(scala212, scala3)

javacOptions ++= Seq(
  "-encoding",
  "UTF-8"
)
scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-encoding",
  "UTF-8"
) ++
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, _)) => Seq("-Xsource:3")
    case _            => Seq.empty
  })
// There are some "unused" settings from sbt-git, disabling this check to not pollute logs
Global / lintUnusedKeysOnLoad := false

// Installs a git pre-push hook that runs `just code-format-check-all`
// Skips on Windows and when not in a git checkout (e.g. CI shallow clones still
// have .git, but published-sources extracts don't)
Global / onLoad ~= { old =>
  if (!scala.util.Properties.isWin) {
    import java.nio.file._
    val hooksDir = Paths.get(".git", "hooks")
    if (Files.isDirectory(hooksDir.getParent)) {
      val prePush = hooksDir.resolve("pre-push")
      val script =
        """#!/bin/sh
          |set -eu
          |if ! command -v just >/dev/null 2>&1; then
          |  echo "pre-push: 'just' is not installed, skipping format check" >&2
          |  exit 0
          |fi
          |just code-format-check-all
          |""".stripMargin
      Files.createDirectories(hooksDir)
      Files.write(prePush, script.getBytes("UTF-8"))
      prePush.toFile.setExecutable(true)
    }
  }
  old
}

// Publishing settings
organization := "me.seroperson"
licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/seroperson/jvm-live-reload"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/seroperson/jvm-live-reload"),
    "scm:git:git://github.com/seroperson/jvm-live-reload.git",
    Some("scm:git:ssh://git@github.com/seroperson/jvm-live-reload.git")
  )
)
developers := List(
  Developer(
    id = "seroperson",
    name = "Daniil Sivak",
    email = "seroperson@gmail.com",
    url = url("https://seroperson.me/")
  )
)

commands ++= Seq(Commands.quickPublish, Commands.catVersion)

addCommandAlias(
  "quickLocalPublish",
  "quickPublish;publishM2;publishLocal;catVersion"
)

// if version was pinned already, read from file, otherwise generate new
version := {
  val versionFile = file("version.txt")
  if (versionFile.exists()) {
    IO.read(versionFile).trim
  } else {
    version.value
  }
}

lazy val javaProjectSettings = Seq(
  crossScalaVersions := List(scala212),
  crossPaths := false,
  autoScalaLibrary := false,
  Compile / unmanagedSourceDirectories := (Compile / javaSource).value :: Nil
)

LocalRootProject / name := "root"
LocalRootProject / publish / skip := true
LocalRootProject / publishLocal / skip := true
LocalRootProject / publishM2 / skip := true

lazy val `sbtLiveReload` = (projectMatrix in file("sbt"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    name := "sbt-live-reload",
    description := "Provides an universal Live Reload experience for web applications built with sbt",
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.12.0"
        case _      => "2.0.0-RC10"
      }
    },
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "me.seroperson.reload.live.sbt",
    libraryDependencies ++= {
      scalaBinaryVersion.value match {
        case "3" => Seq(
          "me.seroperson" %% "sbt-testkit" % "0.1.0" % Test,
          "org.scala-sbt" %% "protocol" % "2.0.0-RC12" % Test,
          "org.scalatest" %% "scalatest" % "3.2.19" % Test,
          "io.grpc" % "grpc-netty-shaded" % "1.72.0" % Test,
          "io.grpc" % "grpc-stub" % "1.72.0" % Test,
          "io.grpc" % "grpc-protobuf" % "1.72.0" % Test,
          "io.grpc" % "grpc-services" % "1.72.0" % Test,
        )
        case _ => Seq.empty
      }
    },
    resolvers += Resolver.mavenLocal,
    dependencyOverrides ++= {
      scalaBinaryVersion.value match {
        case "3" => Seq("org.scala-sbt" %% "protocol" % "2.0.0-RC12")
        case _   => Seq.empty
      }
    },
    Test / fork := true,
    Test / testForkedParallel := false,
    // Only compile/run tests for Scala 3 (sbt-testkit is Scala 3 only)
    Test / unmanagedSourceDirectories := {
      scalaBinaryVersion.value match {
        case "3" => Seq((Test / scalaSource).value)
        case _   => Seq.empty
      }
    }
  )
  .jvmPlatform(scalaVersions = supportedScalaSbtVersions)
  .dependsOn(`buildLink`, `runner`)

lazy val `webserver` = (project in file("core/webserver"))
  .settings(javaProjectSettings)
  .settings(
    name := "jvm-live-reload-webserver",
    description := "Development-mode proxy webserver for Live Reload experience on JVM",
    libraryDependencies := Seq(Dependencies.undertow)
  )
  .dependsOn(`buildLink`)

lazy val `webserver-grpc` = (project in file("core/webserver-grpc"))
  .settings(javaProjectSettings)
  .settings(
    name := "jvm-live-reload-webserver-grpc",
    description := "Development-mode GRPC proxy for Live Reload experience on JVM",
    libraryDependencies := Seq(
      Dependencies.grpcNettyShaded,
      Dependencies.grpcStub,
      Dependencies.grpcServices
    )
  )
  .dependsOn(`buildLink`)

lazy val `runner` = (project in file("core/runner"))
  .settings(javaProjectSettings)
  .settings(
    name := "jvm-live-reload-runner",
    description := "Contains an universal Live Reload webserver initialization and reloading logic",
    libraryDependencies := Seq(Dependencies.playFileWatch, Dependencies.jline)
  )
  .dependsOn(`buildLink`)

lazy val `buildLink` = (project in file("core/build-link"))
  .settings(javaProjectSettings)
  .settings(
    name := "jvm-live-reload-build-link",
    description := "Contains classes which shared between build system and application runtime"
  )

lazy val `hookScala` = (projectMatrix in file("core/hook-scala"))
  .settings(
    name := "jvm-live-reload-hook-scala",
    description := "Predefined set of hooks for popular Scala webframeworks",
    libraryDependencies := Seq(
      Dependencies.zio % Provided,
      Dependencies.catsEffect % Provided
    ) ++ (scalaBinaryVersion.value match {
        // https://github.com/sbt/sbt/issues/8328
        case "3" => Seq("org.scala-lang" %% "scala3-library" % scalaVersion.value)
        case _   => Seq.empty
      })
  )
  .jvmPlatform(scalaVersions = supportedScalaVersions)
  .dependsOn(`buildLink`)
