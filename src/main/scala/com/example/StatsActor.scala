package com.example

import akka.actor.{Actor, ActorLogging, Props}

class StatsActor extends Actor with ActorLogging {

  var sessionStats = Seq.empty[SessionStats]

  override def receive: Receive = {
    case stats: SessionStats =>
      sessionStats = stats +: sessionStats
    case EOS =>
      log.info(
        s"""
           |== STATS ==
           |Top 2 Landing pages : {}
           |Top 1 Sink page : {}
           |Top 3 Browser : {}
           |Top 3 Referrer : {}
        """.stripMargin,
        sessionTopByAggr(2, (s: SessionStats) => s.requestsHistory.headOption.map(_.url).toSeq),
        sessionTopByAggr(1, (s: SessionStats) => s.requestsHistory.lastOption.map(_.url).toSeq),
        sessionTopByAggr(3, (s: SessionStats) => s.requestsHistory.map(_.browser)),
        sessionTopByAggr(3, (s: SessionStats) => s.requestsHistory.map(_.referrer))
      )
  }

  private def sessionTopByAggr[T](n: Int, aggregateFn: SessionStats => Seq[T]) = {
    sessionStats
      .flatMap(aggregateFn(_))
      .groupBy(identity)
      .mapValues(_.size)
      .toSeq.sortBy(-_._2).take(n).toMap
  }

}

case object PrintStats

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
