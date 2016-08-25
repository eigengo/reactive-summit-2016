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
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class HandleSummaryBuilderTest extends FlatSpec with PropertyChecks with Matchers {
  import scala.concurrent.duration._

  it should "handle single item" in {
    val builder = new HandleSummaryBuilder("@honzam399")
    val summary = builder.appendAndBuild(InternalMessage("@honzam399", 0, "a", Scene(labels = Seq(Scene.Label("beer", 1.0)))))

    summary.handle shouldBe "@honzam399"
    summary.items should have size 1
    summary.items.head.windowSize shouldBe 0
    summary.items.head.description shouldBe "beer"
  }

  it should "handle multiple items in a single window" in {
    val builder = new HandleSummaryBuilder("@honzam399")
    builder.appendAndBuild(InternalMessage("@honzam399", 1.second.toMicros, "a", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    builder.appendAndBuild(InternalMessage("@honzam399", 2.second.toMicros, "b", Scene(labels = Seq(Scene.Label("cake", 1.0)))))
    builder.appendAndBuild(InternalMessage("@honzam399", 3.second.toMicros, "c", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    val summary = builder.appendAndBuild(InternalMessage("@honzam399", 4.second.toMicros, "d", Identity(identifiedFaces = Seq(Identity.IdentifiedFace("Jamie Allen")))))

    summary.handle shouldBe "@honzam399"
    summary.items should have size 1
    summary.items.head.windowSize shouldBe 3.second.toMillis
    summary.items.head.description shouldBe "with the famous Jamie Allen and beer, cake"
  }

  it should "window tweets properly" in {
    val builder = new HandleSummaryBuilder("@honzam399")
    builder.appendAndBuild(InternalMessage("@honzam399", 1.minute.toMicros, "a", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    val summary = builder.appendAndBuild(InternalMessage("@honzam399", 2.minute.toMicros, "b", Scene(labels = Seq(Scene.Label("beer", 1.0)))))

    summary.items should have size 2
  }

}
