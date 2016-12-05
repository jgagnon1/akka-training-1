package com.example

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.actor.Actor.Receive

import scala.collection.mutable

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy extends Actor {

  var userSessionActors = Map.empty[Long, ActorRef]

  override def receive: Receive = {
    case r@Request(sessionId, timestamp, _, _, _) =>
      val userSessionActor = userSessionActors.getOrElse(sessionId, {
        val sessionActor = context.actorOf(SessionActor.props())
        userSessionActors += (sessionId -> sessionActor)
        context.watch(userSessionActor)
        sessionActor
      })
      userSessionActor ! r

    case Terminated(terminatedActor) =>
      // FIXME : Optimize find with BiMap
      userSessionActors
        .find { case ((_, ref)) => ref == terminatedActor }
        .foreach { case (sessionId, _) => userSessionActors -= sessionId }
  }

}

object RequestProxy {

  def props() = Props[RequestProxy]

}

