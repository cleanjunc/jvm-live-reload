import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import scala.concurrent.Await
import scala.concurrent.duration._

object App {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("pekko-http-app")
    import system.dispatcher

    val route =
      concat(
        path("greet") {
          get {
            complete("Hello World")
          }
        },
        path("health") {
          get {
            complete(StatusCodes.OK)
          }
        }
      )

    val binding = Await.result(
      Http().newServerAt("0.0.0.0", me.seroperson.BuildInfo.port).bind(route),
      30.seconds
    )

    // Block the main thread until live-reload interrupts it, then gracefully
    // terminate the ActorSystem so its dispatcher threads exit before reload.
    try {
      Await.result(system.whenTerminated, Duration.Inf)
    } catch {
      case _: InterruptedException =>
        Await.result(binding.unbind(), 30.seconds)
        Await.result(system.terminate(), 30.seconds)
    }
  }
}
