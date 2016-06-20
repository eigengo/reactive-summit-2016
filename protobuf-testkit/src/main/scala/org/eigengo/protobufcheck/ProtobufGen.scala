package org.eigengo.protobufcheck

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

object X {

  def inout[A <: GeneratedMessage with Message[A]](in: GeneratedMessage, out: GeneratedMessageCompanion[A]): A = {
    val os = new ByteArrayOutputStream()
    in.writeTo(os)

    out.parseFrom(new ByteArrayInputStream(os.toByteArray))
  }

}
