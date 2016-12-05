package com.example

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import akka.actor.Actor.Receive

import scala.concurrent.duration._

class SessionActor extends Actor {

  var requestsHistory = Seq.empty[Request]

  override def receive: Receive = {
    case r: Request =>
      requestsHistory = r +: requestsHistory
      context.setReceiveTimeout(5 seconds)
    case ReceiveTimeout =>
      // TODO : send stats to StatsActors


      context.stop(self)
  }


}

object SessionActor {
  def props(): Props = Props(classOf[SessionActor])

}
