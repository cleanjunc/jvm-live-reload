updateOptions := updateOptions.value.withLatestSnapshots(false)

resolvers += Resolver.mavenLocal

addSbtPlugin("me.seroperson" % "sbt-live-reload" % sys.props("project.version"))
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("nl.gn0s1s" % "sbt-dotenv" % "3.2.0")
libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client4" %% "core" % "4.0.12",
  "com.eed3si9n.expecty" %% "expecty" % "0.17.1"
)
