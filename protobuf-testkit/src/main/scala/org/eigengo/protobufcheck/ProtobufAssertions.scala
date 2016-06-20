package org.eigengo.protobufcheck

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.google.protobuf.Descriptors.FieldDescriptor
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import org.scalatest.Assertions

trait ProtobufAssertions extends Assertions {

  def assertForwardCompatible[A <: GeneratedMessage with Message[A]](in: GeneratedMessage, out: GeneratedMessageCompanion[A]): Unit = {
    def fdEquals(a: FieldDescriptor)(b: FieldDescriptor): Boolean = {
      a.getName == b.getName && a.getType.name() == b.getType.name()
    }

    import collection.JavaConversions._

    val os = new ByteArrayOutputStream()
    in.writeTo(os)

    val inFields = in.companion.descriptor.getFields.toList
    val outFields = out.descriptor.getFields.toList
    val pairs = inFields.flatMap { inD =>
      outFields.find(fdEquals(inD)).map { outD => (inD, outD) }
    }

    val outM = out.parseFrom(new ByteArrayInputStream(os.toByteArray))

    pairs.foreach {
      case (inD, outD) =>
        val inV = in.getField(inD)
        val outV = outM.getField(outD)
        assert(inV.equals(outV))
    }
  }

}
