/*
 * The Reactive Summit Austin talk
 * Copyright (C) 2016 Jan Machacek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.eigengo.rsa.dashboard.v100

import akka.actor.{Props, ReceiveTimeout}
import akka.stream.actor.ActorPublisher
import com.trueaccord.scalapb.GeneratedMessage

object EventsPerHandleActor {
  def props(handle: String): Props = Props(classOf[EventsPerHandleActor], handle)
}

class EventsPerHandleActor(handle: String) extends ActorPublisher[String] {
  private var messages: List[GeneratedMessage] = Nil

  @scala.throws(classOf[Exception])
  override def preStart(): Unit = {
    import scala.concurrent.duration._

    context.system.eventStream.subscribe(self, classOf[(String, GeneratedMessage)])
    context.setReceiveTimeout(120.seconds)
  }

  @scala.throws(classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case (`handle`, message: GeneratedMessage) ⇒
      messages = message :: messages
      onNext(messages.toString())
    case ReceiveTimeout ⇒
      onCompleteThenStop()
  }

}
