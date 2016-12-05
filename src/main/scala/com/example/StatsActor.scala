package com.example

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props}

class StatsActor extends Actor {

  var sessionStats = Seq.empty[SessionStats]

  override def receive: Receive = {
    case stats: SessionStats =>
      sessionStats = stats +: sessionStats

    case EOS =>
      summarize()
  }

  def summarize() = {

    print(avgNumOfReqPerBrowserPerMinute)

  }

  def avgNumOfReqPerBrowserPerMinute: Map[String, Long] = {
    val allRequests: Seq[Request] = sessionStats.map(_.requestsHistory).flatten
    val browserToRequestTime: Map[String, Seq[Long]] = allRequests
      .map(request => (request.browser, request.timestamp))
      .groupBy(_._1)
      .mapValues(all => all.map(_._2))

    val browserToCountPerMin: Map[String, Long] = browserToRequestTime.mapValues(times => times.size / Math.max(1, ((times.max - times.min) / 1000 / 60)))
    browserToCountPerMin
  }


}

object StatsActor {

  def props(): Props = Props(classOf[StatsActor])

}
