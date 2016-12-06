package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.actor.Actor.Receive
import com.example.RequestProxy.LiveStats

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy(statsActor: ActorRef) extends Actor with ActorLogging {

  var userSessionActors = Map.empty[Long, ActorRef]

  override def receive: Receive = liveProcessing(LiveStats.empty)

  def liveProcessing(liveStats: LiveStats): Receive = handleMessages(liveStats) orElse handleLiveStatusRequest(liveStats)

  def handleMessages(liveStats: LiveStats): Receive = {
    case r@Request(sessionId, _, _, _, _) =>
      val userSessionActor = userSessionActors.getOrElse(sessionId, {
        log.info("New session detected with session id : {}", sessionId)
        val sessionActor = context.actorOf(SessionActor.props(sessionId, statsActor))
        userSessionActors += (sessionId -> sessionActor)
        sessionActor
      })

      context.watch(userSessionActor)
      userSessionActor forward r

      // Update live stats
      context.become(liveProcessing(liveStats.copy(eventProcessed = liveStats.eventProcessed + 1)))

    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

      // Update live stats
      context.become(liveProcessing(liveStats.copy(completedSessions = liveStats.completedSessions + 1)))

    case EOS =>
      userSessionActors.values.foreach {
        _ forward EOS
      }
      context.become(streamFinished orElse handleOfflineStatusRequest)
  }

  def streamFinished: Receive = {
    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

      if (userSessionActors.isEmpty) {
        statsActor forward EOS
      }
  }

  private def handleLiveStatusRequest(liveStats: LiveStats): Receive = {
    case NumberOfOpenSessions => sender ! userSessionActors.size
    case NumberOfCompletedSessions => sender ! liveStats.completedSessions
    case NumberOfEventsProcessed => sender ! liveStats.eventProcessed
  }

  private def handleOfflineStatusRequest: Receive = {
    case NumberOfOpenSessions => sender ! userSessionActors.size
    case NumberOfCompletedSessions => statsActor forward NumberOfCompletedSessions
    case NumberOfEventsProcessed => statsActor forward NumberOfEventsProcessed
  }

  private def removeSessionActors(terminatedActor: ActorRef): Unit = {
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

  final case class LiveStats(completedSessions: Int, eventProcessed: Int)

  object LiveStats {
    def empty = LiveStats(0, 0)
  }

}

