package com.example

import akka.actor.{Actor, Props}

import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by jerome on 2016-12-05.
  */
class EventReader extends Actor {

  val RequestRegex: Regex = "Request\\((.*)\\)".r

  override def receive: Receive = {
    case Read(path) =>
      val lines = Source.fromFile(path).getLines()
      val requests = lines.map { line =>
        val RequestRegex(body) = line
        val Array(sessionId, ts, url, ref, browser) = body.split(",")
        Request(sessionId.toLong, ts.toLong, url, ref, browser)
      }
  }

}

object EventReader {

  def props(): Props = Props[EventReader]

}

final case class Request(sessionId: Long, timestamp: Long, url: String, referrer: String, browser: String)

final case class Read(path: String)
