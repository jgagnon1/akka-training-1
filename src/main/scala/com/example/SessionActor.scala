package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}

import scala.concurrent.duration._

class SessionActor(statsActor: ActorRef) extends Actor with ActorLogging {

  var requestsHistory = Seq.empty[Request]

  override def receive: Receive = {
    case r: Request =>
      requestsHistory = r +: requestsHistory
      context.setReceiveTimeout(5 seconds)
    case ReceiveTimeout =>
      log.debug("Receive timeout : End of session -> sending stats")
      sendStats()
      context.stop(self)
    case EOS =>
      log.debug("EOS : End of session -> sending stats")
      sendStats()
      context.stop(self)
  }

  private def sendStats() = requestsHistory.reverse match {
    case xs@h +: _ => statsActor ! SessionStats(h.sessionId, xs)
    case _ => // Empty history noop
  }

}

object SessionActor {

  def props(statsActor: ActorRef): Props = Props(classOf[SessionActor], statsActor)

}

final case class SessionStats(sessionId: Long, requestsHistory: Seq[Request])
