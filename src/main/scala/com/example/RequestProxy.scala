package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.actor.Actor.Receive

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy(statsActor: ActorRef) extends Actor with ActorLogging {

  var completedSessionsCount = 0L
  var eventsProcessedCount = 0L
  var userSessionActors = Map.empty[Long, ActorRef]

  override def receive: Receive = handleMessages orElse handleStatusRequest

  def handleMessages: Receive = {
    case r@Request(sessionId, timestamp, _, _, _) =>
      eventsProcessedCount += 1
      val userSessionActor = userSessionActors.getOrElse(sessionId, {
        //log.info("New session detected with session id : {}", sessionId)
        val sessionActor = context.actorOf(SessionActor.props(statsActor))
        userSessionActors += (sessionId -> sessionActor)
        sessionActor
      })

      context.watch(userSessionActor)

      userSessionActor forward r

    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

    case EOS =>
      userSessionActors.values.foreach {
        _ forward EOS
      }
      context.become(fileProcessingFinished orElse handleStatusRequest)
  }

  def fileProcessingFinished: Receive = {
    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

      if (userSessionActors.isEmpty) {
        statsActor forward EOS
      }
  }

  private def handleStatusRequest: Receive = {
    case NumberOfOpenSessions => sender ! userSessionActors.size
    case NumberOfCompletedSessions => statsActor forward NumberOfCompletedSessions
    case NumberOfEventsProcessed => statsActor forward NumberOfEventsProcessed
  }

  private def removeSessionActors(terminatedActor: ActorRef): Unit = {
    completedSessionsCount += 1
    // FIXME : Optimize find with BiMap
    userSessionActors
      .find { case ((_, ref)) => ref == terminatedActor }
      .foreach { case (sessionId, _) =>
        log.info("End of session detected for session id: {}", sessionId)
        userSessionActors -= sessionId
      }
  }

}

object RequestProxy {

  def props(statsActor: ActorRef) = Props(classOf[RequestProxy], statsActor)

}

