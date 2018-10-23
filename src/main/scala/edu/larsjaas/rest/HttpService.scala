package edu.larsjaas.rest

import akka.actor.{Actor, Props}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import org.slf4j.LoggerFactory

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

class HttpService extends Actor {
  val log = HttpService.log

  override def preStart(): Unit = {
    log.info("born")
    super.preStart()
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("died")
  }

  def receive = {
    case HttpRequest(method, uri, headers, entity, protocol) =>
      Try(onHttpRequest(method, uri, headers, entity, protocol)) match {
        case Success(x) =>
        case Failure(err) =>
          log.error("exception handling http-request: ", err)
      }
    case x =>
      log.warn(s"unhandled message ${x.getClass.getSimpleName} - $x")
  }

  def onHttpRequest(method: HttpMethod, uri: Uri, headers: Seq[HttpHeader], entity: RequestEntity, protocol: HttpProtocol): Unit = {
    val path = uri.path.toString()
    //log.debug(s"path: $path")
    Try(getClass.getResourceAsStream(path)) match {
      case Success(in) =>
        if (in != null) {
          val content = new BufferedSource(in).mkString
          val headers = RawHeader("Content-Disposition", s"""inline; filename="${path}"""") :: Nil
          sender ! HttpResponse(StatusCodes.OK, headers, HttpEntity(ContentTypes.`text/html(UTF-8)`, content))
          in.close
        }
        else {
          log.warn(s"reading resource error: $path")
          sender ! HttpResponse(status = StatusCodes.NotFound)
        }
      case x =>
        log.warn(s"reading resource: ${x.getClass.getSimpleName} - $x")
        sender ! HttpResponse(status = StatusCodes.NotFound)
    }
  }

}

object HttpService {
  val NAME = "HttpService"
  val log = LoggerFactory.getLogger(NAME)

  def props(): Props = {
    Props.create(classOf[HttpService])
  }
}
