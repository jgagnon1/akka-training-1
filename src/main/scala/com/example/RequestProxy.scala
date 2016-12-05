package com.example

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive

/**
  * Created by jerome on 2016-12-05.
  */
class RequestProxy extends Actor {

  override def receive: Receive = {
    case request: Request =>
      println(s"Received request: $request")
    case Die =>
      context.stop(self)
  }

}

object RequestProxy {

  def props() = Props[RequestProxy]

}

case object Die
