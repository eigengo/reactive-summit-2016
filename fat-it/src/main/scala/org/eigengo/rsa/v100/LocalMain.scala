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
package org.eigengo.rsa.v100

import java.io.File

import scala.io.Source

object LocalMain {

  def main(args: Array[String]): Unit = {
    System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    System.setProperty("CASSANDRA_JOURNAL_CPS", "localhost:9042")
    System.setProperty("CASSANDRA_SNAPSHOT_CPS", "localhost:9042")
    Source.fromFile(new File(System.getProperty("user.home"), ".env/twitter-rsa"))
      .getLines()
      .foreach { line ⇒
        val Array(k, v) = line.split("=")
        System.setProperty(k, v)
      }

//    org.eigengo.rsa.ingest.v100.Main.main(args)
//    org.eigengo.rsa.dashboard.v100.Main.main(args)
//    org.eigengo.rsa.scene.v100.Main.main(args)
//    org.eigengo.rsa.identity.v100.Main.main(args)
//
//    Thread.sleep(30000)
    org.eigengo.rsa.it.v100.Main.main(args)
  }

}
