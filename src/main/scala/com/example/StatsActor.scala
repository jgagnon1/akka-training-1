package com.example

import akka.actor.{Actor, ActorLogging, Props}
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

import scala.collection.immutable.ListMap
import scala.collection.mutable

class StatsActor extends Actor with ActorLogging {

  var sessionStats = Seq.empty[SessionStats]

  override def receive: Receive = aggregating orElse handleStatusRequest

  private def aggregating: Receive = {
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
      log.info("Busiest minutes by Request Count: {}", globalCountByAggr((r: Request) =>
        DateTimeFormat.forPattern("YYYY-MM-dd HH:mm").print(r.timestamp)))
      log.info("Average time per URLs in milliseconds: {}", avgTimePerURL)

      context.become(handleStatusRequest)
  }

  private def handleStatusRequest: Receive = {
    case NumberOfCompletedSessions => sender ! sessionStats.size
    case NumberOfEventsProcessed => sender ! sessionStats.map(_.requestsHistory.size).sum
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
    val results = allRequest
      .groupBy(aggregateFn)
      .map { case (k, v) => (k, v.size) }
      .toSeq.sortBy(-_._2)

    ListMap(results: _*)
  }

  private def avgTimePerURL = {
    val urlMap = mutable.Map.empty[String, (Long, Long)]
    sessionStats.map(s => s.requestsHistory).foreach {requests =>
      requests.sliding(2).foreach {
        case first +: second +: Nil =>
          val url = first.url
          val value = urlMap.getOrElse(url, (0L, 0L))
          value match {
            case (sum, count) =>
              urlMap.update(first.url, (sum + (second.timestamp - first.timestamp), count + 1L))
          }
        case _ => //Noop
      }
    }
    urlMap.map {
      case (url, (sum, count)) =>
        (url, sum / count.toFloat)
    }
  }

}

case object PrintStats

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
