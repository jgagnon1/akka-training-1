package com.example

import akka.actor.{Actor, Props, Stash, Terminated}
import akka.actor.Actor.Receive

class ChatActorManager extends Actor with Stash {

  override def receive: Receive = idle

  private def idle: Receive = {
    case StartChat(sessionId) =>
      context.become(busy)
      val chatActor = context.actorOf(ChatActor.props(sessionId))
      context.watch(chatActor)
  }

  private def busy: Receive = {
    case StartChat(sessionId) =>
      stash()
    case Terminated(_) =>
      context.become(idle)
      unstashAll()
  }

}

object ChatActorManager {
  def props() = Props(classOf[ChatActorManager])
}



final case class StartChat(sessionId: Long)

