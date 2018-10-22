package edu.larsjaas

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import edu.larsjaas.rest.{CrudServiceFactoryActor, WebService}

class Context() {
  val log = LoggerFactory.getLogger("Context")

  var system: Option[ActorSystem] = None

  def start(): Unit = {
    log.info("started")
    val config: Config = ConfigFactory.load("application")
    val cruds = config.getStringList("ember-test.crud.categories").asScala

    system = Some(ActorSystem("ember-test", config))
    var crudhub = system.get.actorOf(CrudServiceFactoryActor.props(cruds))

    log.info("cruds: " + cruds)
    log.info("contains 'device'? " + cruds.contains("device"))

    val webactor = system.get.actorOf(WebService.props(crudhub, cruds), WebService.NAME)
  }

  def stop(): Unit = {
    log.info("killed")
  }
}

object Boot {
  var context: Option[Context] = None

  def main(args: Array[String]): Unit = {
    val command = args.length match {
      case 0 => "start"
      case _ => args(0)
    }
    command match {
      case "start" =>
        context = Some(new Context())
        context.get.start()
      case "stop" =>
        context.foreach(c => {
          c.stop()
          context = None
        })

    }
  }
}