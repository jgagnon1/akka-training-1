package com.example

import akka.actor.ActorSystem

object ApplicationMain extends App {
  val system = ActorSystem("EventReader")

  val requestProxy = system.actorOf(RequestProxy.props())
  val reader = system.actorOf(EventReader.props(requestProxy))

  reader ! Read("resources/events-1k.txt")

  system.awaitTermination()
}