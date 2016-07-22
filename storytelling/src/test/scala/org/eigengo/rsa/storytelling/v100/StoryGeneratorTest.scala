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
package org.eigengo.rsa.storytelling.v100

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class StoryGeneratorTest extends FlatSpec with PropertyChecks with Matchers {

  "Story generator" should "generate stories from the right corpus" in {
    val command = GenerateStory(
      seedWords = List("beer", "cake"),
      corpus = GenerateStory.Corpus.VERYBRITISHPROBLEMS,
      limit = 200,
      locale = Some("en_GB")
    )
    val response = new StoryGenerator().generate(command)

    println(response)

    fail("Finish me")
  }

}
