package edu.larsjaas.rest

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model._
import org.slf4j.LoggerFactory
import akka.pattern.ask
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Failure

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}


class CrudServiceFactoryActor(categories: Seq[String]) extends Actor {
  val log = CrudServiceFactoryActor.log
  var crudservices: Option[Map[String, ActorRef]] = None

  val ADD_WITH_NEW_STRING = true

  override def preStart(): Unit = {
    log.info(s"born")
    crudservices = Some(categories.map(c =>
      c -> context.system.actorOf(CrudServiceActor.props(c), c)
    ).toMap)
    super.preStart()
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info(s"died")
  }

  def receive = {
    case req@HttpRequest(method, uri, headers, entity, protocol) =>
      val zender = sender
      log.debug(s"${method.name} ${uri.toString}")
      method match {
        case HttpMethods.GET =>
          val elts = uri.path.toString().split("/")
          elts.size match {
            case 2 =>
              val crudType = elts(1)
              if (crudservices.get.contains(crudType)) {
                (crudservices.get.get(crudType).get ? GetIndexRequest(Some(crudType)))(20 seconds, self)
                .onComplete {
                  case Success(index: GetIndexResponse) =>
                    var array = new JArray(index.index.toList)
                    val json = compact(render(array))
                    zender ! HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, json))
                  case x =>
                    log.warn("getindex failed")
                }
              }
            case 3 =>
              log.info("get entry")
              val crudType = elts(1)
              val crudId = elts(2)
              if (crudservices.get.contains(crudType)) {
                if (ADD_WITH_NEW_STRING && crudId.equals("new")) {
                  implicit val formats = org.json4s.DefaultFormats
                  val id = UUID.randomUUID.toString
                  val obj = parse(s"""{"id":"${id}"}""")
                  (crudservices.get.get(crudType).get ? NewEntryRequest(Some(id), Some(obj.asInstanceOf[JObject]), Some(crudType))) (20 seconds, self)
                    .onComplete {
                      case Success(entry: NewEntryResponse) =>
                        val json = compact(render(obj))
                        zender ! HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, json))
                      case x =>
                        log.warn("newentry failed")
                    }
                } else {
                  (crudservices.get.get(crudType).get ? GetEntryRequest(crudId, Some(crudType))) (20 seconds, self)
                    .onComplete {
                      case Success(entry: GetEntryResponse) =>
                        entry.entry match {
                          case Some(e) =>
                            val json = compact(render(e))
                            zender ! HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, json))
                          case None =>
                            zender ! HttpResponse(StatusCodes.InternalServerError)
                        }
                      case x =>
                        log.warn("getentry failed")
                    }
                }
              }
            case x =>
              log.warn("unknown path " + uri.path.toString)
          }

        case HttpMethods.POST =>
          val elts = uri.path.toString().split("/")
          elts.size match {
            case 3 =>
              val json = req.entity match {
                case HttpEntity.Strict(contentType, byteString) =>
                  new String(byteString.toArray, "UTF-8")
                case x => ""
              }
              val crudType = elts(1)
              val crudId = elts(2)
              val obj = parse(json)
              if (crudservices.get.contains(crudType)) {
                (crudservices.get.get(crudType).get ? PostEntryRequest(crudId, obj.asInstanceOf[JObject], Some(crudType))) (20 seconds, self)
                  .onComplete {
                    case Success(result) =>
                      zender ! HttpResponse(status = StatusCodes.OK)
                    case Failure(err) =>
                      log.warn(s"post error: ${err.getClass.getSimpleName} - $err")
                      zender ! HttpResponse(status = StatusCodes.InternalServerError)
                  }
              }
            case x =>
              log.warn("unknown path")
          }

        case HttpMethods.PATCH =>
          val elts = uri.path.toString().split("/")
          elts.size match {
            case 3 =>
              val json = req.entity match {
                case HttpEntity.Strict(contentType, byteString) =>
                  new String(byteString.toArray, "UTF-8")
                case x => ""
              }
              val crudType = elts(1)
              val crudId = elts(2)
              val obj = parse(json)
              if (crudservices.get.contains(crudType)) {
                (crudservices.get.get(crudType).get ? PatchEntryRequest(crudId, obj.asInstanceOf[JObject], Some(crudType))) (20 seconds, self)
                  .onComplete {
                    case Success(result) =>
                      zender ! HttpResponse(status = StatusCodes.OK)
                    case Failure(err) =>
                      log.warn(s"post error: ${err.getClass.getSimpleName} - $err")
                      zender ! HttpResponse(status = StatusCodes.InternalServerError)
                  }
              }
            case x =>
              log.warn("unknown path")
          }

        case HttpMethods.DELETE =>
          val elts = uri.path.toString().split("/")
          elts.size match {
            case 3 =>
              val crudType = elts(1)
              val crudId = elts(2)
              if (crudservices.get.contains(crudType)) {
                (crudservices.get.get(crudType).get ? DeleteEntryRequest(crudId, Some(crudType)))(20 seconds, self)
                  .onComplete {
                    case Success(result) =>
                      zender ! HttpResponse(status = StatusCodes.OK)
                    case Failure(err) =>
                      log.warn(s"delete error: ${err.getClass.getSimpleName} - $err")
                      zender ! HttpResponse(status = StatusCodes.InternalServerError)
                  }
              }
          }

        case x =>
          log.warn("unhandled http method: " + method)
      }
    case x =>
      log.warn(s"unhandled message of type ${x.getClass.getSimpleName}")
  }
}

object CrudServiceFactoryActor {
  val NAME = "CrudServiceFactoryActor"
  val log = LoggerFactory.getLogger(NAME)

  def props(categories: Seq[String]): Props = {
    Props.create(classOf[CrudServiceFactoryActor], categories)
  }
}