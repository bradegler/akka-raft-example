package app

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

import pl.project13.scala.akka.raft._
import pl.project13.scala.akka.raft.protocol._
import pl.project13.scala.akka.raft.cluster._

import java.util.concurrent._

object ApplicationMain {

    implicit val timeout = Timeout(10 seconds)

    def main(args: Array[String]): Unit = {
        if (args.isEmpty) {
            Seq("2551","2552","2553").foreach(startup(_))
        }
        else {
            startup(args(0))
        }
    }

    def startup(port: String): Unit = {
        val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").withFallback(ConfigFactory.load())
        val system = ActorSystem("RaftSystem", config)
        val kvStore = system.actorOf(Props[KvStore], name = s"raft-member-$port")
        val clusterActor = system.actorOf(ClusterRaftActor.props(kvStore, 3))
    }
}