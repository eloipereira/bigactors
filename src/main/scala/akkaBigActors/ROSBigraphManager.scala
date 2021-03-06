package akkaBigActors

import java.net.{URI, URISyntaxException}

import akka.actor.ActorRef
import edu.berkeley.eloi.bigraph.{BRR, Bigraph}
import org.apache.commons.logging.Log
import org.ros.address.InetAddressFactory
import org.ros.concurrent.CancellableLoop
import org.ros.message.MessageListener
import org.ros.namespace.GraphName
import org.ros.node.topic.{Publisher, Subscriber}
import org.ros.node.{AbstractNodeMain, ConnectedNode, DefaultNodeMainExecutor, NodeConfiguration, NodeMainExecutor}

import scala.collection.mutable


case class BIGRAPH_REQUEST_WITH_REF(ref: ActorRef)

/**
 * Created by eloi on 09-07-2014.
 */
class ROSBigraphManager(host: String = "localhost", port: Int = 11311) extends BigraphManager {

  val brrQueue: mutable.Queue[BRR] = new mutable.Queue[BRR]()

  override def executeBRR(brr: BRR): Unit = brrQueue.enqueue(brr)

  override def getBigraph: Bigraph = bigraph

  var bigraph = new Bigraph()

  override def preStart = {

    val node: AbstractNodeMain = new AbstractNodeMain {
      override def getDefaultNodeName: GraphName = GraphName.of("bigraphManager/listener")

      override def onStart(connectedNode: ConnectedNode): Unit = {
        val log: Log = connectedNode.getLog
        val subscriber: Subscriber[std_msgs.String] = connectedNode.newSubscriber("bigraph", std_msgs.String._TYPE)
        subscriber.addMessageListener(new MessageListener[std_msgs.String] {
          override def onNewMessage(msg: std_msgs.String): Unit = {
            log.info("[BigraphManager]: received new bgm term message")
            bigraph = new Bigraph(msg.getData + ";")
          }
        })

        val publisher: Publisher[std_msgs.String] = connectedNode.newPublisher("brr", std_msgs.String._TYPE)
        connectedNode.executeCancellableLoop(new CancellableLoop {
          override def loop(): Unit = {
            if(brrQueue.nonEmpty){
              var brrStr: std_msgs.String = publisher.newMessage()
              brrStr.setData(brrQueue.dequeue().toString)
              publisher.publish(brrStr)
            }
            Thread.sleep(1000)
          }
        })

        super.onStart(connectedNode)
      }
    }

    val executor: NodeMainExecutor = DefaultNodeMainExecutor.newDefault
    executor.execute(node, setupConfiguration)
    def getMasterUri: URI = {
      try new URI("http", null, host, port, "/", null, null)
      catch {
        case e: URISyntaxException => null
      }
    }
    def setupConfiguration: NodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback.getHostName, getMasterUri)
    while (bigraph.equals(new Bigraph())) {}
  }
}
