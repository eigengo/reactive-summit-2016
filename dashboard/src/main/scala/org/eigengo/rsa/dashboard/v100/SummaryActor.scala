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

import akka.actor.Props
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request

object SummaryActor {
  lazy val props: Props = Props[SummaryActor]

  implicit object HandleSummaryOrdering extends Ordering[HandleSummary] {
    override def compare(x: HandleSummary, y: HandleSummary): Int = x.handle.compare(y.handle)
  }

}

class SummaryActor extends ActorPublisher[Summary] {
  import SummaryActor._
  private val maximumTopHandles = 100
  private val summary: collection.mutable.Map[String, HandleSummaryBuilder] = collection.mutable.Map()

  @scala.throws(classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[InternalMessage])
  }

  @scala.throws(classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case m@InternalMessage(handle, _, _, _) ⇒
      if (summary.size > maximumTopHandles) {
        summary.find { case (h, b) ⇒ h != handle && !b.isActive(m) }.foreach { case (h, _) ⇒ summary.remove(h) }
      }

      val builder = summary.getOrElse(handle, new HandleSummaryBuilder(handle))
      builder.append(m)
      summary.put(handle, builder)

      onNext(Summary(topHandleSummaries = summary.values.map(_.build()).toSeq.sorted))
    case Request(n) ⇒
      onNext(Summary(topHandleSummaries = summary.values.map(_.build()).toSeq.sorted))
  }

}
