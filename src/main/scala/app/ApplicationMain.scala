package app

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props

import pl.project13.scala.akka.raft._
import pl.project13.scala.akka.raft.protocol._
import pl.project13.scala.akka.raft.cluster._

import java.util.concurrent._

object ApplicationMain {

    def main(args: Array[String]): Unit = {
        if (args.isEmpty)
            startup(Seq("2551", "2552", "2553"))
        else
            startup(args)
    }

    def startup(ports: Seq[String]): Unit = {

        val members = ports.map { port =>
            // Override the configuration of the port
            val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())
            // Create an Akka system
            val system = ActorSystem("RaftSystem", config)
            // Create an actor that handles cluster domain events
            val actor = system.actorOf(Props[ClusterListener], name = s"raft-member-$port")
            val clusterActor = system.actorOf(ClusterRaftActor.props(actor, 3))
            clusterActor
        }
        val raftConfiguration: ClusterConfiguration = ClusterConfiguration(members)
        members foreach { _ ! ChangeConfiguration(raftConfiguration) }
        //clusterActor ! ChangeConfiguration(raftConfiguration)

        val executor = Executors.newSingleThreadScheduledExecutor()
        val system = ActorSystem("RaftSystem")
        def raftMembersPath = system / "raft-member-*"
        val client = system.actorOf(RaftClientActor.props(raftMembersPath), "raft-client")

        executor.scheduleAtFixedRate(new Runnable {
          def run() {
            client ! Broadcast("Are you alive?")
          }
        },1,5,TimeUnit.SECONDS)
    }
}