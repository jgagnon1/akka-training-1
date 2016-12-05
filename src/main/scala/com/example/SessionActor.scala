package com.example

import akka.actor.{Actor, ActorRef, Props, ReceiveTimeout}
import akka.actor.Actor.Receive

import scala.concurrent.duration._

class SessionActor(statsActor: ActorRef) extends Actor {

  var requestsHistory = Seq.empty[Request]

  override def receive: Receive = {
    case r: Request =>
      requestsHistory = r +: requestsHistory
      context.setReceiveTimeout(5 seconds)
    case ReceiveTimeout =>
      requestsHistory match {
        case xs @ h +: _ => statsActor ! SessionStats(h.sessionId, xs)
        case _ => // Empty history noop
      }
      context.stop(self)
  }


}

object SessionActor {
  def props(): Props = Props(classOf[SessionActor])

}

final case class SessionStats(sessionId: Long, requestsHistory: Seq[Request])
