package com.example

import akka.actor.{Actor, ActorLogging, Props}

class StatsActor extends Actor with ActorLogging {

  var sessionStats = Seq.empty[SessionStats]

  override def receive: Receive = {
    case stats: SessionStats =>
      sessionStats = stats +: sessionStats
    case EOS =>
      log.info("== STATS ==")
      log.info("Top 2 Landing pages : {}", sessionTopByAggr(2, (s: SessionStats) => s.requestsHistory.headOption.map(_.url).toSeq))
      log.info("Top 1 Sink page : {}", sessionTopByAggr(1, (s: SessionStats) => s.requestsHistory.lastOption.map(_.url).toSeq))
      log.info("Top 3 Browser : {}", sessionTopByAggr(3, (s: SessionStats) => s.requestsHistory.map(_.browser)))
      log.info("Top 3 Referrer : {}", sessionTopByAggr(3, (s: SessionStats) => s.requestsHistory.map(_.referrer)))
      log.info("View count for URLs : {}", globalCountByAggr((r: Request) => r.url))
      log.info("Browser Stats : {}", globalPctByAggr((r: Request) => r.browser))
  }

  private def allRequest = sessionStats.flatMap(_.requestsHistory)

  private def sessionTopByAggr[T](n: Int, aggregateFn: SessionStats => Seq[T]) = {
    sessionStats
      .flatMap(aggregateFn(_))
      .groupBy(identity)
      .mapValues(_.size)
      .toSeq.sortBy(-_._2).take(n).toMap
  }

  private def globalPctByAggr[T](aggregateFn: Request => T) = {
    globalCountByAggr(aggregateFn)
      .map { case (k, v) => (k, v / allRequest.size.toDouble) }
  }

  private def globalCountByAggr[T](aggregateFn: Request => T) = {
    allRequest
      .groupBy(aggregateFn)
      .map { case (k, v) => (k, v.size) }
      .toSeq.sortBy(-_._2).toMap
  }

}

case object PrintStats

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
