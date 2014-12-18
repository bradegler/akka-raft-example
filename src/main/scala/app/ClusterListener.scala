package app

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor

import pl.project13.scala.akka.raft._

sealed trait Cmnd
case class Broadcast(msg: String) extends Cmnd

class ClusterListener extends RaftActor with ActorLogging {

    type Command = Cmnd

    def apply = {
      case Broadcast(msg) =>
        log.info(s"Received broadcast message $msg")
        msg
    }
}