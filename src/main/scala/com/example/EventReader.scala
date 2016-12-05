package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}

import scala.io.Source
import scala.util.matching.Regex
import scala.concurrent.duration._

/**
  * Created by jerome on 2016-12-05.
  */
class EventReader(requestProxy: ActorRef) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case Read(path) =>
      val requests = Source.fromFile(path).getLines()
        .map(EventReader.parseRequest)

      val (firstTs, lastTs) = requests.foldLeft[(Option[Long], Option[Long])]((None, None)) { case (firstTimestamp, request) =>
        firstTimestamp match {
          case (None, _) =>
            requestProxy ! request
            (Some(request.timestamp), Some(request.timestamp))
          case (Some(first), _) =>
            val delay = request.timestamp - first
            context.system.scheduler.scheduleOnce(delay milliseconds) {
              requestProxy ! request
            }
            (Some(first), Some(request.timestamp))
        }
      }

      val finalDelay: Long = (for {
        f <- firstTs
        l <- lastTs
      } yield l - f).getOrElse(0)

      context.system.scheduler.scheduleOnce(finalDelay milliseconds) {
        log.info("Done with processing message : sending EOS")
        requestProxy ! EOS
        context.stop(self)
      }
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

case object EOS
