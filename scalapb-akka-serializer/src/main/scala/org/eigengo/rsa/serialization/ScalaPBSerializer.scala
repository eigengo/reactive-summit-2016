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
package org.eigengo.rsa.serialization

import akka.actor.ExtendedActorSystem
import akka.serialization.SerializerWithStringManifest
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

class ScalaPBSerializer(system: ExtendedActorSystem) extends SerializerWithStringManifest {

  override val identifier: Int = 0xface0fb0

  override def manifest(o: AnyRef): String = o.getClass.getCanonicalName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case g: GeneratedMessage ⇒ g.toByteArray
    case _ ⇒ throw new IllegalArgumentException(s"$o is not an instance of ScalaPB-generated class.")
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef =
    system.dynamicAccess.getObjectFor[GeneratedMessageCompanion[_ <: GeneratedMessage with Message[_]]](manifest)
      .flatMap(_.validate(bytes))
      .get

}
