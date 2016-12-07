package com.example

import akka.actor.{Actor, ActorLogging, Props}

import scala.Console._
import scala.io.StdIn

/**
  * Created by jerome on 2016-12-06.
  */
class ChatActor(sessionId: Long) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    context.become(retrivingUsername)
    self ! TryUser
  }

  override def receive: Receive = Actor.emptyBehavior

  private def retrivingUsername: Receive = {
    case TryUser =>
      println("Hello, what's your name?")
      self ! UserInput(StdIn.readLine())
    case UserInput(message) =>
      examineMessage(message, retrievingUserRequest(message))
  }

  private def retrievingUserRequest(username: String): Receive = {
    case TryUser =>
      println(s"$username, how can I help you?")
      self ! UserInput(StdIn.readLine())
    case UserInput(message) =>
      examineMessage(message, suggestingFAQ(username, message))
  }

  private def suggestingFAQ(username: String, question: String): Receive = {
    case TryUser =>
      println(s"$username, to solve your question '$question', have you tried our FAQ session?")
      self ! UserInput(StdIn.readLine().toLowerCase)
    case UserInput("no") =>
      println(s"$username, please check our FAQ page here: http://oursite.com/FAQ Goodbye!")
      context.stop(self)
    case UserInput("yes") =>
      println(s"$username, we will have a human CA contact you. Goodbye!")
      context.stop(self)
    case _ =>
      self ! TryUser
  }


  private def examineMessage(message: String, nextState: Receive) = {
    message.trim match {
      case "" =>
        self ! TryUser
      case _ =>
        context.become(nextState)
        self ! TryUser
    }
  }

}

object ChatActor {
  def props(sessionId: Long) = Props(classOf[ChatActor], sessionId)
}

final case class UserInput(message: String)

object TryUser

