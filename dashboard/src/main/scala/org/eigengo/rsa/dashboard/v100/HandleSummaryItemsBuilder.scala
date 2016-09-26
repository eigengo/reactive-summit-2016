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

import com.trueaccord.scalapb.GeneratedMessage
import org.eigengo.rsa.identity.v100.Identity
import org.eigengo.rsa.scene.v100.Scene
import org.eigengo.rsa.{identity, scene}

object HandleSummaryItemsBuilder {
  implicit object TweetEnvelopeOrdering extends Ordering[TweetEnvelope] {
    override def compare(x: TweetEnvelope, y: TweetEnvelope): Int = x.ingestionTimestamp.compare(y.ingestionTimestamp)
  }
}

class HandleSummaryItemsBuilder(maximumMessages: Int = 500) {
  import HandleSummaryItemsBuilder._

  import scala.concurrent.duration._

  private var messages = List.empty[TweetEnvelope]

  private def acceptableIngestionTimestampDiff(m1: TweetEnvelope)(m2: TweetEnvelope): Boolean =
    math.abs(m1.ingestionTimestamp - m2.ingestionTimestamp) < 30.seconds.toNanos

  def isActive(lastIngestedMessage: TweetEnvelope): Boolean =
    messages.lastOption.forall(acceptableIngestionTimestampDiff(lastIngestedMessage))

  def build(): List[HandleSummary.Item] = {
    def messageFromEnvelope(envelope: TweetEnvelope): Option[GeneratedMessage] = {
      (envelope.version, envelope.messageType) match {
        case (100, "identity") ⇒ identity.v100.Identity.validate(envelope.payload.toByteArray).toOption
        case (100, "scene") ⇒ scene.v100.Scene.validate(envelope.payload.toByteArray).toOption
        case _ ⇒ None
      }
    }

    def itemFromWindow(window: List[TweetEnvelope]): Option[HandleSummary.Item] = {
      val windowSize = (window.last.ingestionTimestamp - window.head.ingestionTimestamp).nanos.toMillis.toInt
      val groups = window.flatMap(msg ⇒ messageFromEnvelope(msg)).groupBy(_.getClass)

      val identities = groups
        .get(classOf[Identity])
        .map(_.asInstanceOf[List[Identity]])
        .map(_.foldLeft((List.empty[Identity.IdentifiedFace], List.empty[Identity.UnknownFace])) {
          case ((i, u), f) ⇒ f.face match {
            case Identity.Face.IdentifiedFace(value) ⇒ ((value :: i).distinct, u)
            case Identity.Face.UnknownFace(value) ⇒ (i, (value :: u).distinct)
            case _ ⇒ (i, u)
          }
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

      if (sb.nonEmpty) Some(HandleSummary.Item(windowSize, sb.toString(), Nil)) else None
    }

    def transformMessages(): List[HandleSummary.Item] = {
      val windows = messages.foldLeft(List.empty[List[TweetEnvelope]]) {
        case (Nil, msg) ⇒ List(List(msg))
        case (nel, msg) if acceptableIngestionTimestampDiff(nel.last.last)(msg) ⇒ nel.init :+ (nel.last :+ msg)
        case (nel, msg) ⇒ nel :+ List(msg)
      }

      windows.flatMap(itemFromWindow)
    }

    transformMessages()
  }

  def append(message: TweetEnvelope): Unit = {
    if (!messages.exists(_.messageId == message.messageId)) {
      messages = (message :: messages).takeRight(maximumMessages).sorted
    }
  }

}
