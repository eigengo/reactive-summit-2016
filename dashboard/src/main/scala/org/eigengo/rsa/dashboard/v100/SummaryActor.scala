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
import akka.persistence.PersistentActor
import org.eigengo.rsa.Envelope

object SummaryActor {
  lazy val props: Props = Props[SummaryActor]

  case object Peek

  implicit object HandleSummaryOrdering extends Ordering[HandleSummary] {
    override def compare(x: HandleSummary, y: HandleSummary): Int = x.handle.compare(y.handle)
  }

}

class SummaryActor extends PersistentActor {
  import SummaryActor._
  private val maximumTopHandles = 100
  private val topHandleHSIBuilders: collection.mutable.Map[String, HandleSummaryItemsBuilder] = collection.mutable.Map()

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[Envelope])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override val persistenceId: String = "summary"

  override def receiveRecover: Receive = {
    case m: Envelope ⇒ handleMessage(m)
  }

  def buildSummary(): Summary = {
    val topHandleSummaries = topHandleHSIBuilders.map {
      case (k, v) ⇒ HandleSummary(handle = k, v.build())
    }.toList.sorted

    Summary(topHandleSummaries = topHandleSummaries)
  }

  override def receiveCommand: Receive = {
    case m: Envelope ⇒ persist(m)(handleMessage)
    case Peek ⇒ context.system.eventStream.publish(buildSummary())
  }

  private def handleMessage(message: Envelope): Unit = {
    if (topHandleHSIBuilders.size > maximumTopHandles) {
      topHandleHSIBuilders
        .find { case (h, b) ⇒ h != message.handle && !b.isActive(message) }
        .foreach { case (h, _) ⇒ topHandleHSIBuilders.remove(h) }
    }

    val builder = topHandleHSIBuilders.getOrElse(message.handle, new HandleSummaryItemsBuilder())
    builder.append(message)
    topHandleHSIBuilders.put(message.handle, builder)

    context.system.eventStream.publish(buildSummary())
  }

}
