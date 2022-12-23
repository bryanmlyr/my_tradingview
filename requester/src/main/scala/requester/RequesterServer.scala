package requester

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.typesafe.config.ConfigFactory
import requester.grpc._

import scala.concurrent.{ExecutionContext, Future}

object RequesterServer {
  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("akka.http.server.preview.enable-http2 = on")
      .withFallback(ConfigFactory.defaultApplication())

    val system = ActorSystem("request", conf)
    new RequesterServer(system).run()
  }
}

class RequesterServer(system: ActorSystem) {
  def run(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem = system
    implicit val ec: ExecutionContext = sys.dispatcher

    val service: HttpRequest => Future[HttpResponse] =
      RequesterServiceHandler(new RequesterServiceImpl())

    val binding = Http().newServerAt("127.0.0.1", 8080).bind(service)

    binding.foreach { binding => println(s"gRPC server bound to: ${binding.localAddress}") }

    binding
  }
}
