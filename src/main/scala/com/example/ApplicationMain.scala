package com.example

import akka.actor.ActorSystem

object ApplicationMain extends App {
  val system = ActorSystem("EventReader")

  val statsActor = system.actorOf(StatsActor.props())
  val requestProxy = system.actorOf(RequestProxy.props(statsActor))
  val reader = system.actorOf(EventReader.props(requestProxy))

  reader ! Read("resources/events-200k.txt")

  //new TerminalInterface(requestProxy, system).run()
}