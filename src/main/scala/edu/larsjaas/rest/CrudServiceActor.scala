package edu.larsjaas.rest

import java.util.UUID

import akka.actor.{Actor, Props}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.collection.mutable

class CrudServiceActor(category: String) extends Actor {
  val log = LoggerFactory.getLogger(CrudServiceActor.NAME + "(" + category + ")")

  val collection: mutable.Map[String, JObject] = mutable.HashMap[String, JObject]()

  override def preStart(): Unit = {
    log.info("born")
    // FIXME: read initial dataset from Config
    super.preStart()
  }

  override def postStop(): Unit = {
    super.postStop()
    log.info("died")
  }

  def receive = {
    case GetIndexRequest(crud) =>
      sender ! GetIndexResponse(collection.values.toList)
    case NewEntryRequest(id, obj, crud) =>
      val newid = id.isEmpty match { case true => UUID.randomUUID().toString case false => id.get }
      val newobj = obj.isEmpty match { case true => parse(s"""{id: "$id"}""") case false => obj.get }
      collection.put(newid, newobj.asInstanceOf[JObject])
      sender ! NewEntryResponse(newid, collection(newid), Some(category))
    case GetEntryRequest(id, _) =>
      sender ! GetEntryResponse(collection.get(id))
    case DeleteEntryRequest(id, _) =>
      collection.remove(id)
      sender ! DeleteEntryResponse(ok = true)
    case PatchEntryRequest(id, obj, _) =>
      collection.get(id) match {
        case Some(orig) =>
          val built = orig.merge(obj) // ++ obj
          collection.put(id, built)
          if (log.isTraceEnabled())
            log.trace("merged object: " + compact(render(built)))
          sender ! PatchEntryResponse(ok = true)
        case None =>
          collection.put(id, obj)
      }
      sender ! PatchEntryResponse(ok = true)
    case PostEntryRequest(id, obj, _) =>
      collection.put(id, obj)
      sender ! PostEntryResponse(ok = true)
    case x =>
      log.warn(s"unhandled message of type ${x.getClass.getSimpleName}")
  }

}

object CrudServiceActor {
  val NAME = "CrudServiceActor"
  val log = LoggerFactory.getLogger(NAME)

  def props(category: String): Props = {
    Props.create(classOf[CrudServiceActor], category)
  }
}