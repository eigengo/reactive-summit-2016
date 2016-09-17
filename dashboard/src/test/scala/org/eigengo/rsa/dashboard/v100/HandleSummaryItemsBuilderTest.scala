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

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.GeneratedMessage
import org.eigengo.rsa.identity.v100.Identity
import org.eigengo.rsa.scene.v100.Scene
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class HandleSummaryItemsBuilderTest extends FlatSpec with PropertyChecks with Matchers {
  import scala.concurrent.duration._

  private def pue(ingestionTimestamp: Long, messageId: String, messageType: String, message: GeneratedMessage): TweetEnvelope = {
    val payload = ByteString.copyFrom(message.toByteArray)
    TweetEnvelope(version = 100, ingestionTimestamp = ingestionTimestamp, handle = "@honzam399", messageType = messageType, messageId = messageId, payload = payload)
  }

  it should "handle single item" in {
    val builder = new HandleSummaryItemsBuilder()
    builder.append(pue(ingestionTimestamp = 0, messageId = "a", messageType = "scene", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    val items = builder.build()

    items should have size 1
    items.head.windowSize shouldBe 0
    items.head.description shouldBe "beer"
  }

  it should "handle multiple items in a single window" in {
    val builder = new HandleSummaryItemsBuilder()
    builder.append(pue(ingestionTimestamp = 10.second.toNanos, messageId = "a", messageType = "scene", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    builder.append(pue(ingestionTimestamp = 20.second.toNanos, messageId = "b", messageType = "scene", Scene(labels = Seq(Scene.Label("cake", 1.0)))))
    builder.append(pue(ingestionTimestamp = 30.second.toNanos, messageId = "c", messageType = "scene", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    builder.append(pue(ingestionTimestamp = 40.second.toNanos, messageId = "d", messageType = "identity", Identity(identifiedFaces = Seq(Identity.IdentifiedFace("Jamie Allen")))))
    val items = builder.build()

    items should have size 1
    items.head.windowSize shouldBe 30.second.toMillis
    items.head.description shouldBe "with the famous Jamie Allen and beer, cake"
  }

  it should "window tweets properly" in {
    val builder = new HandleSummaryItemsBuilder()
    builder.append(pue(ingestionTimestamp = 1.minute.toNanos, messageId = "a", messageType = "scene", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    builder.append(pue(ingestionTimestamp = 2.minute.toNanos, messageId = "b", messageType = "scene", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    val items = builder.build()

    items should have size 2
  }

}
