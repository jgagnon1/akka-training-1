package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import akka.actor.SupervisorStrategy.Stop
import com.example.RequestProxy.LiveStats
import com.example.SessionFilterActor.SessionRequestException

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy(statsActor: ActorRef, chatActorManager: ActorRef) extends Actor with ActorLogging {

  var userSessionActors = Map.empty[Long, ActorRef]

  override val supervisorStrategy = OneForOneStrategy() {
    case _: SessionRequestException => Stop
  }

  override def receive: Receive = liveProcessing(LiveStats.empty)

  def liveProcessing(liveStats: LiveStats): Receive = handleMessages(liveStats) orElse handleLiveStatusRequest(liveStats)

  def handleMessages(liveStats: LiveStats): Receive = {
    case r@Request(sessionId, _, _, _, _) =>
      val userSessionActor = userSessionActors.getOrElse(sessionId, {
        //log.info("New session detected with session id : {}", sessionId)
        val sessionFilterActor = context.actorOf(SessionFilterActor.props(sessionId, statsActor, chatActorManager))
        userSessionActors += (sessionId -> sessionFilterActor)
        sessionFilterActor
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
        //log.info("End of session detected for session id: {}", sessionId)
        userSessionActors -= sessionId
      }
  }

}

object RequestProxy {

  def props(statsActor: ActorRef, chatActorManager: ActorRef) = Props(classOf[RequestProxy], statsActor, chatActorManager)

  final case class LiveStats(completedSessions: Int, eventProcessed: Int)

  object LiveStats {
    def empty = LiveStats(0, 0)
  }

}

