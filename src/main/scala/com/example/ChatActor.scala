package com.example

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}

/**
  * Created by jerome on 2016-12-06.
  */
class ChatActor(sessionId: Long) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"Starting chat session with <$sessionId>")
  }

  override def receive: Receive = Actor.emptyBehavior

}

object ChatActor {

  def props(sessionId: Long) = Props(classOf[ChatActor], sessionId)

}
