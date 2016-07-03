package org.eigengo.protobufcheck

import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.FieldDescriptor
import com.trueaccord.scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}
import org.scalacheck.{Arbitrary, Gen}

object ProtobufGen {

  def message[M <: GeneratedMessage with Message[M]](companion: GeneratedMessageCompanion[M]): Gen[M] = {
    import collection.JavaConversions._

    def generatorsFromFields(fields: List[FieldDescriptor]): List[(FieldDescriptor, Gen[Any])] = {
      fields.map { field ⇒
        import Descriptors.FieldDescriptor._

        val fieldGen: Gen[Any] = field.getType match {
          case Type.BOOL ⇒ Arbitrary.arbBool.arbitrary
          case Type.BYTES ⇒ Gen.containerOf(Arbitrary.arbByte.arbitrary)
          case Type.DOUBLE ⇒ Arbitrary.arbDouble.arbitrary
          case Type.ENUM ⇒ ???
          case Type.FIXED32 ⇒ ???
          case Type.FIXED64 ⇒
          case Type.FLOAT ⇒ Arbitrary.arbFloat.arbitrary
          case Type.GROUP ⇒ ???
          case Type.INT32 ⇒ Arbitrary.arbInt.arbitrary //.withFilter(_ >= 0)
          case Type.INT64 ⇒ Arbitrary.arbBigInt.arbitrary //.withFilter(_ >= 0)
          case Type.MESSAGE ⇒ existentialMessage(companion.messageCompanionForField(field))
          case Type.SFIXED32 ⇒ ???
          case Type.SFIXED64 ⇒ ???
          case Type.SINT32 ⇒ Arbitrary.arbInt.arbitrary
          case Type.SINT64 ⇒ Arbitrary.arbInt.arbitrary
          case Type.STRING ⇒ Arbitrary.arbString.arbitrary
          case Type.UINT32 ⇒ Arbitrary.arbInt.arbitrary //.withFilter(_ >= 0)
          case Type.UINT64 ⇒ Arbitrary.arbBigInt.arbitrary //.withFilter(_ >= 0)
        }

        if (field.isRepeated) {
          (field, Gen.listOf(fieldGen))
        } else {
          (field, fieldGen)
        }
      }
    }

    def existentialMessage(companion: GeneratedMessageCompanion[_]): Gen[_] = {
      val oneOfFields = companion.descriptor.getOneofs.toList.flatMap(_.getFields.toList)
      val fields = companion.descriptor.getFields.toList

      val generators = generatorsFromFields(fields.filterNot(oneOfFields.contains)) ++
                       generatorsFromFields(oneOfFields)

      val (ffd, ffg) = generators.head
      val firstFieldGenerator = ffg.map { x ⇒ Map(ffd → x) }
      val remainingFieldGenerators = generators.tail

      val mappedGenerator = remainingFieldGenerators.foldLeft(firstFieldGenerator) {
        case (result, (fd, fg)) ⇒
          result.flatMap { m ⇒ fg.map(x ⇒ Map(fd → x) ++ m) }
      }

      mappedGenerator.map(companion.fromFieldsMap)
    }

    existentialMessage(companion).map(_.asInstanceOf[M])
  }

}
