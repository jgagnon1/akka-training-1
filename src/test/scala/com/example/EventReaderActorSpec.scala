package com.example


import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll


class EventReaderActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("EventReaderActorSpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  "An EventReader actor" must {
    "send one request per event to RequestProxy" in {
      val requestProxyTestProbe = TestProbe()
      val eventReaderActorRef = system.actorOf(EventReader.props(requestProxyTestProbe.ref))

      eventReaderActorRef ! Read("resources/events-2.txt")
      requestProxyTestProbe.expectMsg(Request(795253081L, 1480534410848L, "/", "google", "firefox"))
      requestProxyTestProbe.expectMsg(Request(795253081L, 1480534410955L, "/", "google", "firefox"))
      requestProxyTestProbe.expectMsg(EOS)
      eventReaderActorRef should be('Terminated)
    }


  }

}
