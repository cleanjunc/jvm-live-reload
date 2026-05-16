import io.grpc.ServerBuilder
import scala.concurrent.ExecutionContext
import greeter._

object App {
  def main(args: Array[String]): Unit = {
    implicit val ec: ExecutionContext = ExecutionContext.global
    
    val server = ServerBuilder
      .forPort(me.seroperson.BuildInfo.port)
      .addService(
        GreeterGrpc.bindService(new GreeterImpl, ec)
      )
      .build()
      .start()
    
    println(s"Server started on port ${me.seroperson.BuildInfo.port}")
    
    server.awaitTermination()
  }
}

class GreeterImpl extends GreeterGrpc.Greeter {
  override def sayHello(request: HelloRequest): scala.concurrent.Future[HelloReply] = {
    scala.concurrent.Future.successful(
      HelloReply(message = s"Hello, ${request.name}!")
    )
  }
}
