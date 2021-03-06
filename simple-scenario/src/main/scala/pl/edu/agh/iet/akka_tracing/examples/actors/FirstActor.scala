package pl.edu.agh.iet.akka_tracing.examples.actors

import akka.actor.{Actor, ActorRef, Props}
import pl.edu.agh.iet.akka_tracing.TracedActor

object FirstActor {
  def props(actorRef: ActorRef): Props = Props(classOf[FirstActor], actorRef)
}

class FirstActor(val actorRef: ActorRef) extends Actor with TracedActor {
  override def receive: Receive = {
    case a =>
      actorRef ! a
  }
}
