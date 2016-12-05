package com.example

import akka.actor.ActorSystem

object ApplicationMain extends App {
  val system = ActorSystem("EventReader")

  val reader = system.actorOf(EventReader.props())

  reader ! Read("resources/events-1k.txt")

  system.awaitTermination()
}