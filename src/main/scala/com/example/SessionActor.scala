package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout}

import scala.concurrent.duration._

class SessionActor(sessionId: Long, statsActor: ActorRef) extends Actor with ActorLogging {

  var requestsHistory = Seq.empty[Request]

  override def receive: Receive = receiveStateHelp(onHelp = false)

  def receiveStateHelp(onHelp: Boolean): Receive = {
    case r@Request(_, _, path, _, _) =>
      requestsHistory = r +: requestsHistory

      // Handle Help chat session
      path match {
        case "/help" if !onHelp =>
          context.become(receiveStateHelp(onHelp = true))
          context.setReceiveTimeout(2 seconds)
        case _ =>
          resetState()
      }
    case ReceiveTimeout if onHelp =>
      val chatActor = context.actorOf(ChatActor.props(sessionId))
      resetState()
    case ReceiveTimeout =>
      //log.debug("Receive timeout : End of session -> sending stats")
      sendStats()
      context.stop(self)
    case EOS =>
      //log.debug("EOS : End of session -> sending stats")
      sendStats()
      context.stop(self)
  }

  private def sendStats() =
    statsActor ! SessionStats(sessionId, requestsHistory.reverse)

  private def resetState() = {
    context.become(receiveStateHelp(onHelp = false))
    context.setReceiveTimeout(5 seconds)
  }

}

object SessionActor {

  def props(sessionId: Long, statsActor: ActorRef): Props = Props(classOf[SessionActor], sessionId, statsActor)

}

final case class SessionStats(sessionId: Long, requestsHistory: Seq[Request])
