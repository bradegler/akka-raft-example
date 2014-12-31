package app

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.RootActorPath
import akka.actor.ActorPath

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import akka.util.Timeout

import akka.cluster.{Member, Cluster}
import akka.cluster.ClusterEvent._
import concurrent.duration._

import pl.project13.scala.akka.raft.cluster.ClusterRaftActor

class ClusterListener(raftActor: ActorRef) extends Actor with ActorLogging {

    val cluster = Cluster(context.system)

    override def preStart(): Unit = {
        super.preStart()
        log.info("Joining new actor to cluster")
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
                log.info("Self added to cluster, starting RaftCluster")
                val clusterActor = context.system.actorOf(ClusterRaftActor.props(raftActor, 3))
            }
        case UnreachableMember(m) =>
            log.info("Member {} detected as unreachable", m)
        case MemberRemoved(m, ps) =>
            log.info("Member {} has been removed after {}", m, ps)
        case _: MemberEvent =>
    }
}

object ClusterListener {
    def props(raftActor: ActorRef) = Props(classOf[ClusterListener], raftActor)
}