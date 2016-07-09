package org.eigengo.rsa.linter

import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}

class MarshallerLinterPlugin(val global: Global) extends Plugin {
  plugin ⇒

  override val name: String = "Marshaller Linter"
  override val description: String = "Verifies the coding standards of marshalling code"
  override val components: List[PluginComponent] = List(component)

  private object component extends PluginComponent {
    override val global: Global = plugin.global
    override val phaseName: String = "marshaller-linter"
    override val runsBefore = List("patmat")
    override val runsAfter: List[String] = List("typer")

    import global._

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      private val permitTypeName = TypeName("org.eigengo.rsa.ScalaPBMarshalling.permit")
      private val rejectedRhsTypes =
        List("akka.http.scaladsl.marshalling.Marshaller", "akka.http.scaladsl.unmarshalling.Unmarshaller")
          .map(name ⇒ rootMirror.getClassIfDefined(name).tpe.erasure)
      private type Rejection = String

      private def mapRhs(rhs: Tree): Option[Rejection] = {
        if (rhs.tpe <:< definitions.NullTpe) None
        else rejectedRhsTypes.find(rejectedType ⇒ rhs.tpe.dealiasWiden.erasure <:< rejectedType).map(_.toString())
      }

      override def apply(unit: CompilationUnit): Unit = {
        def allTrees(tree: Tree): Iterator[Tree] =
          Iterator(tree, analyzer.macroExpandee(tree)).filter(_ != EmptyTree)
            .flatMap(t ⇒ Iterator(t) ++ t.children.iterator.flatMap(allTrees))

        def permit(annotaitonInfo: AnnotationInfo): Boolean =
          annotaitonInfo.matches(permitTypeName)

        def suppressedTree(tree: Tree) = tree match {
          case Annotated(annot, arg) ⇒
            global.inform("A>>>>>>>>>>>>> " + annot.tpe.annotations.toString())
            Some(arg)
          case typed@Typed(expr, tpt) ⇒
            global.inform("T>>>>>>>>>>>>> " + expr.tpe.annotations.toString())
            Some(typed)
          case md: MemberDef ⇒
            global.inform("M>>>>>>>>>>>>> " + md.symbol.annotations.toString())
            Some(md)
          case _ => None
        }

        val suppressedTrees = allTrees(unit.body).flatMap(suppressedTree).toList

        suppressedTrees.foreach {
          case d@ValDef(mods, _, _, rhs) if !mods.hasAnnotationNamed(permit) ⇒
            mapRhs(rhs).foreach { rejection ⇒
              global.globalError(d.pos, s"Cannot hand-roll val of type $rejection.")
            }
          case d@DefDef(mods, _, _, _, _, rhs) if mods.isImplicit ⇒
            mapRhs(rhs).foreach { rejection ⇒
              global.inform(d.pos, d.tpe.annotations.toString())
              global.globalError(d.pos, s"Cannot hand-roll implicit def returning $rejection.")
            }
          case _ ⇒ // noop
        }
      }
    }

  }
}
