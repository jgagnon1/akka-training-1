package com.example

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props}

class StatsActor extends Actor {

  override def receive: Receive = Actor.emptyBehavior

}

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
