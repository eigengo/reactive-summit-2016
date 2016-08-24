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

  it should "handle single item" in {
    val builder = new HandleSummaryBuilder("@honzam399")
    val summary = builder.appendAndBuild(InternalMessage("@honzam399", 1, "a", Scene(labels = Seq(Scene.Label("beer", 1.0)))))

    summary.handle shouldBe "@honzam399"
    summary.items should have size 1
    summary.items.head.windowSize shouldBe 0
    summary.items.head.description shouldBe "beer"
  }

  it should "handle multiple items in a single window" in {
    val builder = new HandleSummaryBuilder("@honzam399")
    builder.appendAndBuild(InternalMessage("@honzam399", 100L * 1000 * 1000, "a", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    builder.appendAndBuild(InternalMessage("@honzam399", 200L * 1000 * 1000, "b", Scene(labels = Seq(Scene.Label("cake", 1.0)))))
    builder.appendAndBuild(InternalMessage("@honzam399", 300L * 1000 * 1000, "c", Scene(labels = Seq(Scene.Label("beer", 1.0)))))
    val summary = builder.appendAndBuild(InternalMessage("@honzam399", 400L * 1000 * 1000, "d", Identity(identifiedFaces = Seq(Identity.IdentifiedFace("Jamie Allen")))))

    summary.handle shouldBe "@honzam399"
    summary.items should have size 1
    summary.items.head.windowSize shouldBe 300
    summary.items.head.description shouldBe "with the famous Jamie Allen and beer, cake"
  }

}
