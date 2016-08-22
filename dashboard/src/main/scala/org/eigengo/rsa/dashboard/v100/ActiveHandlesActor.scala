package org.eigengo.rsa.dashboard.v100

import akka.actor.{Props, ReceiveTimeout}
import akka.stream.actor.ActorPublisher

object ActiveHandlesActor {
  lazy val props: Props = Props[ActiveHandlesActor]
}

class ActiveHandlesActor extends ActorPublisher[List[String]] {
  private var activeHandles: List[String] = Nil

  override def receive: Receive = {
    case (handle: String, _) ⇒
      activeHandles = handle :: activeHandles
      onNext(activeHandles)
    case ReceiveTimeout ⇒
      onCompleteThenStop()
  }

}
