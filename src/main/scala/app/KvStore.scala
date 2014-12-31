package app

import java.util.concurrent.ConcurrentHashMap

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.ActorLogging
import akka.actor.Actor

import pl.project13.scala.akka.raft._

sealed trait Cmnd
case class SetKey(key: String, value: String) extends Cmnd
case class GetKey(key: String) extends Cmnd

class KvStore extends RaftActor with ActorLogging {

    type Command = Cmnd

    val map = new ConcurrentHashMap[String, String]()

    def apply = {
        case SetKey(key, value) =>
            map.put(key, value)
            log.info(s"Set: $key -> $value")
        case GetKey(key) =>
            val res = map.get(key)
            log.info(s"Get: $key -> $res")
            res
    }
}