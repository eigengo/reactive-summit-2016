package org.eigengo.protobufcheck

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.google.protobuf.Descriptors.FieldDescriptor
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import org.scalatest.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}


trait ProtobufMatchers extends Matchers {

  private type Right = GeneratedMessageCompanion[_ <: GeneratedMessage with Message[_]]

  private class CompatibleMatcher(right: Right) extends BeMatcher[GeneratedMessage] {

    private def fdEquals(a: FieldDescriptor)(b: FieldDescriptor): Boolean = {
      a.getName == b.getName && a.getType.name() == b.getType.name()
    }

    import collection.JavaConversions._

    override def apply(left: GeneratedMessage): MatchResult = {
      val os = new ByteArrayOutputStream()
      left.writeTo(os)

      val inFields = left.companion.descriptor.getFields.toList
      val outFields = right.descriptor.getFields.toList
      val pairs = inFields.flatMap { inD =>
        outFields.find(fdEquals(inD)).map { outD => (inD, outD) }
      }

      val outM = right.parseFrom(new ByteArrayInputStream(os.toByteArray))

      val failures = pairs.foldLeft(List.empty[(FieldDescriptor, FieldDescriptor, Any, Any)]) {
        case (r, (inD, outD)) =>
          val inV = left.getField(inD)
          val outV = outM.getField(outD)
          if (!inV.equals(outV)) {
            (inD, outD, inV, outV) :: r
          } else r
      }

      if (failures.isEmpty) {
        MatchResult(matches = true, "", "")
      } else {
        MatchResult(matches = false, "", "")
      }
    }
  }

  def forwardCompatibleWith(right: Right): BeMatcher[GeneratedMessage] = new CompatibleMatcher(right)

  def backwardCompatibleWith(right: Right): BeMatcher[GeneratedMessage] = new CompatibleMatcher(right)

}
