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

import java.util.UUID

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKitBase}
import com.google.protobuf.ByteString
import com.trueaccord.scalapb.GeneratedMessage
import com.typesafe.config.ConfigFactory
import org.eigengo.rsa.identity.v100.Identity
import org.eigengo.rsa.scene.v100.Scene
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class SummaryActorTest extends FlatSpec with TestKitBase with PropertyChecks with Matchers {
  implicit lazy val system = ActorSystem("test", ConfigFactory.load("dashboard-test.conf").resolve())

  it must "handle" in {
    val lsa = TestActorRef[LastSummaryActor]
    system.eventStream.subscribe(lsa, classOf[Summary])

    val scene = ("scene", Scene(labels = Seq(Scene.Label(label = "salad", score = 1))))
    val identity = ("identity", Identity(identifiedFaces = Seq(Identity.IdentifiedFace(name = "Jan", score = 1))))

    def envelopeForHandle(ingestionTimestamp: Long, handle: String, m: (String, GeneratedMessage)): TweetEnvelope = {
      val (messageType, message) = m
      TweetEnvelope(version = 100,
        ingestionTimestamp = ingestionTimestamp,
        handle = handle,
        messageType = messageType,
        messageId = UUID.randomUUID().toString,
        payload = ByteString.copyFrom(message.toByteArray)
      )
    }

    val sa = system.actorOf(SummaryActor.props, "summary")
    (0 until 1)
      .flatMap(x ⇒ List(envelopeForHandle(x, "@" + x, scene), envelopeForHandle(x, "@" + x, identity)))
      .foreach(sa.!)

    Thread.sleep(10000)

    println(lsa.underlyingActor.lastSummary)
    ()
  }

}

class LastSummaryActor extends Actor {
  var lastSummary: Summary = _
  override def receive: Receive = {
    case s: Summary ⇒ lastSummary = s
  }
}
