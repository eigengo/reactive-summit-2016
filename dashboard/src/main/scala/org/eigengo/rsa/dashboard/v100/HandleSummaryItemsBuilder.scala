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

import org.eigengo.rsa.identity.v100.Identity
import org.eigengo.rsa.scene.v100.Scene

import scala.collection.SortedSet

object HandleSummaryItemsBuilder {
  implicit object InternalMessageOrdering extends Ordering[InternalMessage] {
    override def compare(x: InternalMessage, y: InternalMessage): Int = x.ingestionTimestamp.compare(y.ingestionTimestamp)
  }
}

class HandleSummaryItemsBuilder(maximumMessages: Int = 500) {
  import HandleSummaryItemsBuilder._
  import scala.concurrent.duration._

  private var messages = SortedSet.empty[InternalMessage]

  private def acceptableIngestionTimestampDiff(m1: InternalMessage)(m2: InternalMessage): Boolean =
    math.abs(m1.ingestionTimestamp - m2.ingestionTimestamp) < 30.seconds.toNanos

  def isActive(lastIngestedMessage: InternalMessage): Boolean =
    messages.lastOption.forall(acceptableIngestionTimestampDiff(lastIngestedMessage))

  def build(): List[HandleSummary.Item] = {
    def itemFromWindow(window: List[InternalMessage]): HandleSummary.Item = {
      val windowSize = (window.last.ingestionTimestamp - window.head.ingestionTimestamp).nanos.toMillis.toInt
      val groups = window.map(_.message).groupBy(_.getClass)

      val identities = groups
        .get(classOf[Identity])
        .map(_.asInstanceOf[List[Identity]])
        .map(_.foldLeft((List.empty[Identity.IdentifiedFace], List.empty[Identity.UnknownFace])) {
          case ((i, u), f) ⇒ ((i ++ f.identifiedFaces).distinct, (u ++ f.unknownFaces).distinct)
        })

      val sceneLabels = groups
        .get(classOf[Scene])
        .map(_.asInstanceOf[List[Scene]])
        .map(_.foldLeft(List.empty[String]) { (result, scene) ⇒ result ++ scene.labels.map(_.label) }.distinct)

      val sb = new StringBuilder()

      identities.foreach { case (identifiedFaces, unknownFaces) ⇒
        if (identifiedFaces.nonEmpty || unknownFaces.nonEmpty) sb.append("with")
        if (identifiedFaces.nonEmpty) sb.append(s" the famous ${identifiedFaces.map(_.name).mkString(", ")}")
        if (unknownFaces.nonEmpty) sb.appendAll(s" ${unknownFaces.length} other people")
      }

      sceneLabels.foreach { labels ⇒
        if (sb.nonEmpty) sb.append(" and ")
        if (labels.nonEmpty) sb.append(s"${labels.mkString(", ")}")
      }

      HandleSummary.Item(windowSize, sb.toString(), Nil)
    }

    def transformMessages(): List[HandleSummary.Item] = {
      val windows = messages.foldLeft(List.empty[List[InternalMessage]]) {
        case (Nil, msg) ⇒ List(List(msg))
        case (nel, msg) if acceptableIngestionTimestampDiff(nel.last.last)(msg) ⇒ nel.init :+ (nel.last :+ msg)
        case (nel, msg) ⇒ nel :+ List(msg)
      }

      windows.map(itemFromWindow)
    }

    transformMessages()
  }

  def append(message: InternalMessage): Unit = {
    if (!messages.exists(_.messageId == message.messageId)) {
      messages = (messages + message).takeRight(maximumMessages)
    }
  }

}
