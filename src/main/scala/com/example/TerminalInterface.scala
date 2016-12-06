package com.example

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.Console.println
import scala.concurrent.Future
import scala.io.StdIn
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class TerminalInterface(requestProxyActor: ActorRef, system: ActorSystem) {

  implicit private val waitDuration: Timeout = 5 seconds

  implicit private class PrintableFuture[T](future: Future[T]) {
    import scala.concurrent.Await

    def printAwait: String = Try(Await.result(future, waitDuration.duration)) match {
      case Success(r) => r.toString
      case Failure(e) => s"ERROR : ${e.getMessage}"
    }

  }

  def run(): Unit = {
    import Console._

    printMan()

    while (true) {
      println("------------")
      print("> ")
      StdIn.readLine() match {
        case "1" => println(getNumberOfOpenSessions.printAwait)
        case "2" => println(getNumberOfCompletedSessions.printAwait)
        case "3" => println(getNumberOfEventsProcessed.printAwait)
        case "man" => printMan()
        case "q" =>
          println("Exiting the system..")
          system.shutdown()
          system.awaitTermination(10 seconds)
          sys.exit()
        case _ =>
      }
    }
  }

  private def printMan(): Unit = {
    println("Please make your choice:")
    println("1 - Number of Open Sessions")
    println("2 - Number of Completed Sessions")
    println("3 - Number of Events Processed")
    println("man - Manual")
    println("q - Quit")
  }


  private def getNumberOfOpenSessions: Future[Int] = {
    (requestProxyActor ? NumberOfOpenSessions).mapTo[Int]
  }

  private def getNumberOfCompletedSessions: Future[Int] = {
    (requestProxyActor ? NumberOfCompletedSessions).mapTo[Int]
  }

  private def getNumberOfEventsProcessed: Future[Int] = {
    (requestProxyActor ? NumberOfEventsProcessed).mapTo[Int]
  }


}

sealed trait StatusRequest

object NumberOfOpenSessions extends StatusRequest

object NumberOfCompletedSessions extends StatusRequest

object NumberOfEventsProcessed extends StatusRequest
