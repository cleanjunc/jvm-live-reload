val ScalaPBVersion = "0.11.17"
val GrpcVersion = "1.72.0"

enablePlugins(LiveReloadPlugin)
enablePlugins(BuildInfoPlugin)

scalaVersion := "2.13.16"
resolvers += Resolver.mavenLocal

Compile / PB.targets := Seq(
  scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
)

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % GrpcVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % ScalaPBVersion
)

val isSbt2 = settingKey[Boolean]("isSbt2")
isSbt2 := (sbtBinaryVersion.value match {
  case "2" => true
  case _   => false
})

val proxyPort = settingKey[Int]("proxyPort")
proxyPort := (if (isSbt2.value) 9001 else 9000)

val port = settingKey[Int]("port")
port := (if (isSbt2.value) 8081 else 8080)

liveServerType := GrpcServerType

liveDevSettings := Seq(
  DevSettingsKeys.LiveReloadProxyGrpcPort -> proxyPort.value.toString,
  DevSettingsKeys.LiveReloadGrpcPort -> port.value.toString,
  DevSettingsKeys.LiveReloadIsDebug -> "true"
)

buildInfoKeys := Seq[BuildInfoKey](port)
buildInfoPackage := "me.seroperson"

InputKey[Unit]("verifyGrpcCall") := {
  import io.grpc.ManagedChannelBuilder
  import com.eed3si9n.expecty.Expecty.assert
  
  val args = Def.spaceDelimited("<expected_response>").parsed
  val expectedResponse = args.head
  
  val channel = ManagedChannelBuilder
    .forAddress("localhost", proxyPort.value)
    .usePlaintext()
    .build()
  
  try {
    val stub = greeter.GreeterGrpc.blockingStub(channel)
    val request = greeter.HelloRequest(name = "World")
    val response = stub.sayHello(request)
    
    assert(response.message == expectedResponse)
  } finally {
    channel.shutdown()
    channel.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)
  }
}
