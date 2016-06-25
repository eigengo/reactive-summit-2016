package org.eigengo.protobufcheck

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import com.google.protobuf.Descriptors.FieldDescriptor
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import org.scalatest.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}

/**
  * Defines matchers to be used with the ``be`` verb and ScalaPB-generated code. Its matchers
  * provide means to verify that the messages are compatible with each other.
  */
trait ProtobufMatchers extends Matchers {

  private type Right = GeneratedMessageCompanion[_ <: GeneratedMessage with Message[_]]

  /**
    * Common matcher that matches ``GeneratedMessage`` on the left with the ``GeneratedMessage``
    * constructed using the ``GeneratedMessageCompanion`` on the right
    *
    * @param right the right companion
    */
  private class CompatibleMatcher(right: Right) extends BeMatcher[GeneratedMessage] {

    import collection.JavaConversions._

    /**
      * Returns ``true`` if the two ``FieldDescriptor``s are equal
      * @param a the first FD
      * @param b the second FD
      * @return a == b
      */
    protected final def fdEquals(a: FieldDescriptor)(b: FieldDescriptor): Boolean = {
      a.getName == b.getName && a.getType.name() == b.getType.name()
    }

    override def apply(left: GeneratedMessage): MatchResult = {
      val os = new ByteArrayOutputStream()
      left.writeTo(os)

      val leftFields  = left.companion.descriptor.getFields.toList
      val rightFields = right.descriptor.getFields.toList
      val intersectedFields = leftFields.flatMap(l => rightFields.find(fdEquals(l)).map(r => (l, r)))

      val parsedRight = right.parseFrom(new ByteArrayInputStream(os.toByteArray))

      val failures = intersectedFields.foldLeft(List.empty[(FieldDescriptor, FieldDescriptor, Any, Any)]) {
        case (r, (leftDescriptor, rightDescriptor)) =>
          val leftValue  = left.getField(leftDescriptor)
          val rightValue = parsedRight.getField(rightDescriptor)
          if (!leftValue.equals(rightValue)) {
            (leftDescriptor, rightDescriptor, leftValue, rightValue) :: r
          } else r
      }

      if (failures.isEmpty) {
        MatchResult(matches = true, "", "")
      } else {
        MatchResult(matches = false, "", "")
      }
    }
  }

  /**
    * Returns a matcher that verifies that the given ``GeneratedMessage`` is compatible with
    * the value unmarshalled using the ``GeneratedMessageCompanion`` on the right.
    *
    * @param right the companion that can unmarshal from the wire format
    * @return compatiblity matcher
    */
  def compatibleWith(right: Right): BeMatcher[GeneratedMessage] = new CompatibleMatcher(right)

}
