package com.example

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}

import scala.concurrent.duration._

class StatsActor extends Actor with ActorLogging {

  var sessionStats = Seq.empty[SessionStats]

  override def receive: Receive = {
    case stats: SessionStats =>
      sessionStats = stats +: sessionStats
    case EOS =>
      log.info(
        s"""
           |STATS :
           |Top 2 Landing pages : {}
           |Top Sink page : {}
        """.stripMargin,
        topLandingPages(2), topSinkPages(1))
  }

  private def topLandingPages(n: Int) = {
    sessionStats
      .flatMap(_.requestsHistory.headOption.map(_.url))
      .groupBy(identity)
      .mapValues(_.size)
      .toSeq.sortBy(-_._2).take(n).toMap
  }

  private def topSinkPages(n: Int) = {
    sessionStats
      .flatMap(_.requestsHistory.lastOption.map(_.url))
      .groupBy(identity)
      .mapValues(_.size)
      .toSeq.sortBy(-_._2).take(n).toMap
  }

  

}

case object PrintStats

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
