sys.props.get("project.version") match {
  case Some(x) => addSbtPlugin("me.seroperson" % "sbt-live-reload" % x)
  case _ => sys.error("""|The system property 'project.version' is not defined.
                         |Specify this property via -Dproject.version=...""".stripMargin)
}

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"
)
