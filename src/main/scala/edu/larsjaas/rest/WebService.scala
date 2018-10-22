package edu.larsjaas.rest

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings._
import akka.stream.ActorMaterializer
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.pattern.ask

import scala.language.postfixOps

class WebService(crudFactory: ActorRef, crudServices: Seq[String]) extends Actor {
  val log = WebService.log

  implicit val system: ActorSystem = context.system
  implicit val executor: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val routingSettings: RoutingSettings = RoutingSettings(context.system)
  implicit val parserSettings: ParserSettings = ParserSettings(context.system)

  override def preStart(): Unit = {
    log.info("born")
    super.preStart()

    val route = Route {
     extractRequestContext { context =>
       log.info(s"path: ${context.request.uri.path.toString()}")
       path("hello") {
         get {
           complete("Hello World!")
         }
       } ~
       extractRequest { request =>
         val prefix = request.uri.path.toString.split("/")(1)
         if (crudServices.contains(prefix)) {
           toStrictEntity(20 seconds, 10480L) {
             onSuccess((crudFactory ? request) (20 seconds, self)) {
               case result@HttpResponse(_, _, _, _) =>
                 complete(result)
               case x =>
                 log.warn(s"crud factory responded with ${x.getClass.getSimpleName}: $x")
                 complete(HttpResponse(status = StatusCodes.InternalServerError))
             }
           }
         }
         else {
           complete(HttpResponse(status = StatusCodes.InternalServerError))
         }
       }
     }
   }

    val routeFlow = RouteResult.route2HandlerFlow(route)
    val bindingFuture = Http().bindAndHandle(routeFlow, "0.0.0.0", 8080)

    bindingFuture.onComplete {
      case Success(serverBinding) => println(s"listening to ${serverBinding.localAddress}")
      case Failure(error) => println(s"error: ${error.getMessage}")
    }
  }


  override def postStop(): Unit = {
    super.postStop()
    log.info("died")
  }

  def receive = {
    case x =>
      log.warn(s"unhandled message of type ${x.getClass.getSimpleName}")
  }
}

object WebService {
  val NAME = "WebService"
  val log = LoggerFactory.getLogger(NAME)

  def props(crud: ActorRef, crudServices: Seq[String]): Props = {
    Props.create(classOf[WebService], crud, crudServices)
  }
}