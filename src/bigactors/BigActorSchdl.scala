package bigactors

import scala.actors.Actor
import edu.berkeley.eloi.bigraph._
import scala.collection.JavaConversions._
import edu.berkeley.eloi.bgm2java.Debug
import scala.collection.mutable.HashMap

object BigActorSchdl extends Actor{
  var debug = false

  private val hostRelation = new HashMap[Symbol,Symbol]
  private val addressesRelation = new HashMap[Symbol,BigActor]

  start

  def act() {
    Debug.println("Initial bigraph: " + BigraphManager.getBRS,debug)
    loop {
      react{
        case HOSTING(bigActor: BigActor, hostId: Symbol) =>{
          if (BigraphManager.getBRS.getBigraph.getPlaces.contains(new Node(hostId.name))) {
            Debug.println("Hosting BigActor at host " + hostId,debug)
            hostRelation += bigActor.bigActorID -> hostId
            addressesRelation +=  bigActor.bigActorID -> bigActor
            bigActor.start()
          }
          else {
            System.err.println("BigActor cannot be hosted at " + hostId + ". Make sure host exists!")
            System.exit(0)
          }
        }
        case OBSERVE(query, bigActorID) => {
          Debug.println("got a obs request with query " + query + " from "+bigActorID,debug)
          val host = BigraphManager.getBRS.getBigraph.getNode(hostRelation(bigActorID).name)
          val obs = new Observation(SimpleQueryCompiler.generate(query,host,BigraphManager.getBRS.getBigraph))
          Debug.println("Observation: "+obs,debug)
          reply(("OBSERVATION_SUCCESSFUL",obs))
        }
        case CONTROL(brr, bigActorID) => {
          Thread.sleep(3000)
          Debug.println("got a ctr request " + brr,debug)
          if (brr.getRedex.getNodes.contains(BigraphManager.getBRS.getBigraph.getNode(hostRelation(bigActorID).name))
            || brr.getReactum.getNodes.contains(BigraphManager.getBRS.getBigraph.getNode(hostRelation(bigActorID).name))){
            BigraphManager.getBRS.applyRules(List(brr),2)
            Debug.println("New bigraph: " + BigraphManager.getBRS,debug)
          } else {
            System.err.println("Host " + hostRelation(bigActorID) + "is not included on redex/reactum of "+ brr)
            System.exit(0)
          }
        }
        case SEND(msg, bigActorID) => {
          val senderID = bigActorID
          val receiverID = msg.receiverID
          Debug.println("got a snd request from " + bigActorID,debug)
          val senderHost = BigraphManager.getBRS.getBigraph.getNode(hostRelation(senderID).name)
          val destHost = BigraphManager.getBRS.getBigraph.getNode(hostRelation(receiverID).name)
          if (senderHost == destHost || !senderHost.getNames.intersect(destHost.getNames).isEmpty){
            Debug.println("Hosts " + hostRelation(senderID).name + " and " +  hostRelation(receiverID).name + " are connected.",debug)
            addressesRelation(receiverID) ! ("SEND_SUCCESSFUL",msg)
          } else {
            System.err.println("Hosts " + hostRelation(senderID).name + " and " +  hostRelation(receiverID).name + " are not connected.")
            System.exit(0)
          }
        }
        case MIGRATE(newHostId, bigActorID) => {
          Debug.println("got a mgrt request from " + hostRelation(bigActorID) + " to " +newHostId,debug)
          val currentHost = BigraphManager.getBRS.getBigraph.getNode(hostRelation(bigActorID).name)
          val destHost = BigraphManager.getBRS.getBigraph.getNode(newHostId.name)
          if (!currentHost.getNames.intersect(destHost.getNames).isEmpty){
            Debug.println("Hosts connected. Migrating...",debug)
            hostRelation += bigActorID -> newHostId
          } else {
            System.err.println("Hosts " + hostRelation(bigActorID) + " and " + newHostId + " are not connected.")
            System.exit(0)
          }
        }
        case _ => println("UNKNOWN REQUEST")
      }
    }
  }
}