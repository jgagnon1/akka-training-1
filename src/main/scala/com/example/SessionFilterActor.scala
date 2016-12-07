package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}

import scala.concurrent.duration._

class SessionFilterActor(sessionId: Long, statsActor: ActorRef, chatActorManager: ActorRef) extends Actor with ActorLogging {
  import com.example.SessionFilterActor._
  import context.dispatcher

  var requestCountWithinThresholdTime = 0

  val checkRequestFrequency: Cancellable = context.system.scheduler.schedule(
    1 second, 1 second,
    self,
    CheckRequestFrequency
  )

  val sessionActor = context.actorOf(SessionActor.props(sessionId, statsActor, chatActorManager))

  override def receive: Receive = {
    case r: Request =>
      requestCountWithinThresholdTime += 1
      sessionActor.forward(r)

    case CheckRequestFrequency =>
      if(requestCountWithinThresholdTime > RateLimit) {
        log.error("Session {} exceeded rate limit and was discarded", sessionId)
        throw new SessionRequestException
      } else {
        requestCountWithinThresholdTime = 0
      }

    case EOS =>
      if(requestCountWithinThresholdTime > RateLimit) {
        log.error("Session {} exceeded rate limit and was discarded", sessionId)
        throw new SessionRequestException
      } else {
        sessionActor.forward(EOS)
      }
      checkRequestFrequency.cancel()
      context.stop(self)

    case other =>
      sessionActor.forward(other)
  }

  override def postStop(): Unit = {
    log.info(s"SessionFilterActor has been stopped for {}", sessionId)
  }

}

object SessionFilterActor {

  def props(sessionId: Long, statsActor: ActorRef, chatActorManager: ActorRef): Props = Props(classOf[SessionFilterActor], sessionId, statsActor, chatActorManager)

  val RateLimit = 15

  final case class SessionRequestException() extends Exception("Rate limit exceeded`")
  object CheckRequestFrequency

}
