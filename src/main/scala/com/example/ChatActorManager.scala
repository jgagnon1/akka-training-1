package com.example

import akka.actor.{Actor, FSM, Props, Stash, Terminated}

import scala.collection.immutable.Queue

class ChatActorManager extends Actor with FSM[ChatActorManager.State, ChatActorManager.Data] with Stash {
  import ChatActorManager._
  import ChatActorManager.State._

  startWith(Idle, ChatQueue())

  when(Idle) {
    case Event(startChat: StartChat, chatQueue: ChatQueue) =>
      val chatActor = context.actorOf(ChatActor.props(startChat.sessionId))
      context.watch(chatActor)
      goto(Busy)
  }

  when(Busy) {
    case Event(startChat: StartChat, chatQueue: ChatQueue) =>
      stay using chatQueue.copy(chats = chatQueue.chats.enqueue(startChat))
    case Event(Terminated(_), chatQueue: ChatQueue) =>
      chatQueue.chats.dequeueOption match {
        case Some((newChat, newChatQueue)) =>
          self ! newChat
          goto(Idle) using chatQueue.copy(chats = newChatQueue)
        case None =>
          goto(Idle)
      }
  }

  initialize()

}

object ChatActorManager {
  def props() = Props(classOf[ChatActorManager])

  sealed trait State

  object State {

    case object Busy extends State

    case object Idle extends State
  }

  sealed trait Data
  final case class ChatQueue(chats: Queue[StartChat] = Queue.empty) extends Data
}



final case class StartChat(sessionId: Long)

