/**
 * Created by eloi on 03-07-2014.
 */
package akkaBigActors.examples

import java.nio.file.Paths

import akka.actor.{ActorSystem, Props}
import akkaBigActors.{BigActor, BigActorSchdl, BigMCBigraphManager}
import bigactors.examples.RENDEZVOUS_AT_LOCATION

object ExampleAkkaRendezvous2 extends App {
  implicit val system = ActorSystem("mySystem")

  val bigraphManager = system.actorOf(Props(classOf[BigMCBigraphManager], Paths.get(System.getProperty("user.dir")).resolve("src/main/resources/robots.bgm").toString, true))
  val bigraphScheduler = system.actorOf(Props(classOf[BigActorSchdl], bigraphManager))

  val r0BA = system.actorOf(Props(classOf[Leader], 'r0))
  r0BA ! "start"

  class Leader(host: Symbol) extends BigActor(host,bigraphScheduler){

    def receive = {
      case "start" =>
        val rvLoc = PARENT_HOST.head
        val robots = LINKED_TO_HOST
        robots.foreach{r =>
          system.actorOf(Props(classOf[Follower], Symbol(r.getId.toString))) ! RENDEZVOUS_AT_LOCATION(rvLoc)
        }
      case _ => println("unknown message")
    }
  }

  class Follower(host: Symbol) extends BigActor(host,bigraphScheduler){
    def receive = {
      case RENDEZVOUS_AT_LOCATION(loc) => {
        MOVE_HOST_TO(loc)
      }
      case _ => println("unknown message")
    }
  }
}
