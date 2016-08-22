package org.eigengo.rsa.dashboard.v100

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request
import com.trueaccord.scalapb.GeneratedMessage

object ActiveHandlesActor {
  lazy val props: Props = Props[ActiveHandlesActor]
}

class ActiveHandlesActor extends ActorPublisher[List[String]] {
  private var activeHandles: Set[String] = Set.empty

  @scala.throws(classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[(String, GeneratedMessage)])
  }

  @scala.throws(classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case (handle: String, _) ⇒
      activeHandles = activeHandles + handle
      onNext(activeHandles.toList.sorted)
    case Request(n) ⇒
      onNext(activeHandles.toList.sorted)
  }

}
