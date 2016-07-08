package org.eigengo.rsa.linter

import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.{Global, Phase}

class MarshallerLinterPlugin(val global: Global) extends Plugin {
  plugin ⇒
  override val name: String = "Marshaller Linter"
  override val description: String = "Verifies the coding standards of marshalling code"
  override val components: List[PluginComponent] = List(component)

  println("***************")

  private object component extends PluginComponent {
    override val global: Global = plugin.global
    override val phaseName: String = "marshaller-linter"
    override val runsAfter: List[String] = List("typer")

    import global._

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      private val rejectedRhsTypes = List("akka.http.scaladsl.Marshaller", "akka.http.scaladsl.Unmarshaller")
      private type Rejection = String

      private def mapRhs(rhs: Tree): Option[Rejection] = {
        Some("akka.http.scaladsl.Marshaller")
      }

      override def apply(unit: CompilationUnit): Unit = {
        unit.body.foreach {
          case d@ValDef(_, _, _, rhs) ⇒
            mapRhs(rhs).foreach { rejection ⇒
              global.globalError(d.pos, s"Cannot hand-roll val of type $rejection.")
            }
          case d@DefDef(mods, _, _, _, _, rhs) if mods.isImplicit ⇒
            mapRhs(rhs).foreach { rejection ⇒
              global.globalError(d.pos, s"Cannot hand-roll implicit def returning $rejection.")
            }
          case _ ⇒ // noop
        }
      }
    }

  }
}
