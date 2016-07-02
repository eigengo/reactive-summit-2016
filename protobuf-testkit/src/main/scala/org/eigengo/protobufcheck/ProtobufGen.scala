package org.eigengo.protobufcheck

import com.google.protobuf.Descriptors
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import org.scalacheck.{Arbitrary, Gen}

object ProtobufGen {

  def message[M <: GeneratedMessage with Message[M]](companion: GeneratedMessageCompanion[M]): Gen[M] = {
    import collection.JavaConversions._

    val fields = companion.descriptor.getFields.toList
    val generators = fields.map { field ⇒
      import Descriptors.FieldDescriptor._

      val fieldGen: Gen[Any] = field.getType match {
        case Type.BOOL ⇒ Arbitrary.arbBool.arbitrary
        case Type.BYTES ⇒ ???
        case Type.DOUBLE ⇒ Arbitrary.arbDouble.arbitrary
        case Type.ENUM ⇒ ???
        case Type.FIXED32 ⇒ ???
        case Type.FIXED64 ⇒
        case Type.FLOAT ⇒ Arbitrary.arbFloat.arbitrary
        case Type.GROUP ⇒ ???
        case Type.INT32 ⇒ Arbitrary.arbInt.arbitrary //.withFilter(_ >= 0)
        case Type.INT64 ⇒ Arbitrary.arbBigInt.arbitrary //.withFilter(_ >= 0)
        case Type.MESSAGE ⇒ ???
        case Type.SFIXED32 ⇒ ???
        case Type.SFIXED64 ⇒ ???
        case Type.SINT32 ⇒ Arbitrary.arbInt.arbitrary
        case Type.SINT64 ⇒ Arbitrary.arbInt.arbitrary
        case Type.STRING ⇒ Arbitrary.arbString.arbitrary
        case Type.UINT32 ⇒ Arbitrary.arbInt.arbitrary //.withFilter(_ >= 0)
        case Type.UINT64 ⇒ Arbitrary.arbBigInt.arbitrary //.withFilter(_ >= 0)
      }

      (field, fieldGen)
    }
    val (ffd, ffg) = generators.head
    val firstFieldGenerator = ffg.map { x ⇒ Map(ffd → x) }
    val remainingFieldGenerators = generators.tail

    val mappedGenerator = remainingFieldGenerators.foldLeft(firstFieldGenerator) {
      case (result, (fd, fg)) ⇒
        result.flatMap { m ⇒ fg.map(x ⇒ Map(fd → x) ++ m) }
    }

    mappedGenerator.map(companion.fromFieldsMap)
  }

}
