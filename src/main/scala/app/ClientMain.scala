package app

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import akka.cluster.ClusterEvent._
import akka.cluster._

import pl.project13.scala.akka.raft._
import pl.project13.scala.akka.raft.protocol._
import pl.project13.scala.akka.raft.cluster._

import java.lang.management.ManagementFactory

object ClientMain {

    implicit val timeout = Timeout(10 seconds)

    def main(args: Array[String]): Unit = {
        val system = ActorSystem("RaftSystem")
        val cl = system.actorOf(Props(classOf[ClusterListener]))
    }
}


class ClusterListener extends Actor with ActorLogging {
    implicit val timeout = Timeout(10 seconds)
    val cluster = Cluster(context.system)

    override def preStart(): Unit = {
        super.preStart()
        log.info("Joining new cluster listener to cluster")
        cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
    }

    override def postStop(): Unit = {
        cluster unsubscribe self
        super.postStop()
     }

    def receive = {
        case MemberUp(m) if m.hasRole("raft") =>
            log.info("Node joined {} system {}", m, cluster.selfAddress)
            if(m.address == cluster.selfAddress) {
                log.info("Self added to cluster, starting RaftClient")
                val name = ManagementFactory.getRuntimeMXBean().getName()
                val kvStore = context.system.actorOf(Props[KvStore], name = s"impl-raft-member-$name")
                val clusterActor = context.system.actorOf(ClusterRaftActor.props(kvStore, 3), s"raft-member-$name")
                val client = context.system.actorOf(RaftClientActor.props(context.system / "raft-member-*"), "raft-client")
                Await.result(client ? SetKey("test-key1", "test-value-1"), timeout.duration)
                context.system.shutdown
            }
        case UnreachableMember(m) =>
            log.info("Member {} detected as unreachable", m)
        case MemberRemoved(m, ps) =>
            log.info("Member {} has been removed after {}", m, ps)
        case _: MemberEvent =>
    }
}