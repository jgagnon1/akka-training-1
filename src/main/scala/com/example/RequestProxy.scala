package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.actor.Actor.Receive

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy(statsActor: ActorRef) extends Actor with ActorLogging {

  var userSessionActors = Map.empty[Long, ActorRef]

  override def receive: Receive = handleMessages

  def handleMessages: Receive = {
    case r@Request(sessionId, timestamp, _, _, _) =>
      val userSessionActor = userSessionActors.getOrElse(sessionId, {
        log.info("New session detected with session id : {}", sessionId)
        val sessionActor = context.actorOf(SessionActor.props(statsActor))
        userSessionActors += (sessionId -> sessionActor)
        sessionActor
      })

      context.watch(userSessionActor)

      userSessionActor ! r

    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

    case EOS =>
      userSessionActors.values.foreach { _ ! EOS }
      context.become(waitForTermination)
  }

  def waitForTermination: Receive = {
    case Terminated(terminatedActor) =>
      removeSessionActors(terminatedActor)

      if(userSessionActors.isEmpty) {
        statsActor ! EOS
        context.stop(self)
      }
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

}

