package org.eigengo.rsa.v100

import java.io.File

import scala.io.Source

object LocalMain {

  def main(args: Array[String]): Unit = {
    System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
    System.setProperty("CASSANDRA_JOURNAL_CPS", "localhost:9042")
    System.setProperty("CASSANDRA_SNAPSHOT_CPS", "localhost:9042")
    Source.fromFile(new File(System.getProperty("user.home"), ".env/twitter-rsa2016"))
      .getLines()
      .foreach { line ⇒
        val Array(k, v) = line.split("=")
        System.setProperty(k, v)
      }

    //org.eigengo.rsa.ingest.v100.Main.main(args)
    org.eigengo.rsa.dashboard.v100.Main.main(args)
    org.eigengo.rsa.scene.v100.Main.main(args)
    org.eigengo.rsa.identity.v100.Main.main(args)
    org.eigengo.rsa.it.v100.Main.main(args)
  }

}
