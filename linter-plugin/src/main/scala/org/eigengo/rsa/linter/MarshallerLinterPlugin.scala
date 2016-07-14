package org.eigengo.rsa.linter

import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}

/**
  * Reports compile errors in code that attempts to use hand-rolled marshallers
  * @param global the NSC global
  */
class MarshallerLinterPlugin(val global: Global) extends Plugin {
  plugin ⇒

  override val name: String = "Marshaller Linter"
  override val description: String = "Verifies the coding standards of marshalling code"
  override val components: List[PluginComponent] = List(component)

  /**
    * Introduces the ``marshaller-linter`` phase after ``typer``, before ``patmat``.
    */
  private object component extends PluginComponent {
    override val global: Global = plugin.global
    override val phaseName: String = "marshaller-linter"
    override val runsBefore = List("patmat")
    override val runsAfter: List[String] = List("typer")

    import global._

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {

      override def apply(unit: CompilationUnit): Unit = {
        // the permit
        val permitAnnotationType = rootMirror.getClassIfDefined("org.eigengo.rsa.ScalaPBMarshalling.permit").tpe
        // the rejected types; note that they are the erased types, because we don't really want to
        // or need to mess around with variance; checking the erased types is sufficient in this plugin
        val rejectedRhsTypes = List("akka.http.scaladsl.marshalling.Marshaller", "akka.http.scaladsl.unmarshalling.Unmarshaller")
          .map(name ⇒ rootMirror.getClassIfDefined(name).tpe.erasure)

        // Expands all child trees of ``tree``, returning flattened iterator of trees.
        def allTrees(tree: Tree): Iterator[Tree] =
          Iterator(tree, analyzer.macroExpandee(tree)).filter(_ != EmptyTree)
            .flatMap(t ⇒ Iterator(t) ++ t.children.iterator.flatMap(allTrees))

        // checks that the permit annotation is present on the given ``symbol``.
        def hasPermitAnnotation(symbol: global.Symbol): Boolean =
          Option(symbol).forall(_.annotations.exists(_.tpe <:< permitAnnotationType))

        // for now, rejection is the type name that we reject
        type Rejection = String
        // checks the tree for disallowed type
        def rejectHandRolled(tree: Tree): Option[Rejection] = {
          if (tree.tpe <:< definitions.NullTpe) None
          else rejectedRhsTypes.find(rejectedType ⇒ tree.tpe.dealiasWiden.erasure <:< rejectedType).map(_.toString())
          //                                                  ^      ^     ^
          // note the checks are done using the de-aliased, widened and erased types
        }

        // check all expanded trees of each compilation unit; find
        //
        // val x = `rhs`
        // val x: Tpt = `rhs`
        // implicit def x = `rhs`
        // implicit def x: Tpt = `rhs`
        //
        // Check the explicit types or the type of the RHS and reject those that are listed in
        // ``rejectedRhsTypes``.
        //
        allTrees(unit.body).foreach {
          case d@ValDef(mods, _, tpt, rhs) if !hasPermitAnnotation(rhs.symbol) ⇒
            rejectHandRolled(rhs).orElse(rejectHandRolled(tpt)).foreach { rejection ⇒
              global.globalError(d.pos, s"Cannot hand-roll val of type $rejection.")
            }
          case d@DefDef(mods, _, _, _, tpt, rhs) if mods.isImplicit && !hasPermitAnnotation(d.symbol) ⇒
            rejectHandRolled(rhs).orElse(rejectHandRolled(tpt)).foreach { rejection ⇒
              global.globalError(d.pos, s"Cannot hand-roll implicit def returning $rejection.")
            }
          case _ ⇒ // noop
        }
      }
    }

  }
}
