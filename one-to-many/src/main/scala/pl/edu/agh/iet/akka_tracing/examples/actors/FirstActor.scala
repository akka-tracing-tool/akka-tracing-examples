package pl.edu.agh.iet.akka_tracing.examples.actors

import akka.actor.{Actor, ActorRef, Props}
import pl.edu.agh.iet.akka_tracing.TracedActor

object FirstActor {
  def props(actorRefs: Seq[ActorRef]): Props = Props(classOf[FirstActor], actorRefs)
}

class FirstActor(val actorRefs: Seq[ActorRef]) extends Actor with TracedActor {
  override def receive: Receive = {
    case a =>
      for (actorRef <- actorRefs) {
        actorRef ! a
      }
  }
}
