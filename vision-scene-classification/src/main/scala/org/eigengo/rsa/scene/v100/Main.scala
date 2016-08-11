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
package org.eigengo.rsa.scene.v100

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigResolveOptions}

object Main {

  def main(args: Array[String]): Unit = {
    Thread.sleep(30000)
    println("".padTo(80, "*").mkString)
    println(s"Main starting...")
    println("".padTo(80, "*").mkString)

    val config = ConfigFactory.load("application.conf").resolve(ConfigResolveOptions.defaults())
    val system = ActorSystem(name = "scene-classification-100", config = config)
    system.actorOf(SceneClassifierActor.props(config.getConfig("app")))
  }

}
