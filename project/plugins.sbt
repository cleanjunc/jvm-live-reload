resolvers += Resolver.sonatypeCentralSnapshots

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

// https://github.com/sbt/sbt/issues/8328
libraryDependencies ++= (scalaBinaryVersion.value match {
  case "3" => Seq("org.scala-lang" %% "scala3-library" % scalaVersion.value)
  case _   => Seq.empty
})
