package com.example

import akka.actor.{Actor, ActorRef, Props}

import scala.io.Source
import scala.util.matching.Regex

/**
  * Created by jerome on 2016-12-05.
  */
class EventReader(requestProxy: ActorRef) extends Actor {

  override def receive: Receive = {
    case Read(path) =>
      Source.fromFile(path).getLines()
        .map(EventReader.parseRequest)
        .foreach(requestProxy ! _)

      requestProxy ! Die
      context.stop(self)
  }

}

object EventReader {

  def props(requestProxy: ActorRef): Props = Props(classOf[EventReader], requestProxy)

  val RequestRegex: Regex = "Request\\((.*)\\)".r

  def parseRequest(line: String): Request = {
    val RequestRegex(body) = line
    val Array(sessionId, ts, url, ref, browser) = body.split(",")
    Request(sessionId.toLong, ts.toLong, url, ref, browser)
  }

}

final case class Request(sessionId: Long, timestamp: Long, url: String, referrer: String, browser: String)

final case class Read(path: String)
