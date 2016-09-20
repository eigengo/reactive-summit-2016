package org.eigengo.rsa.ingest.v100

import com.trueaccord.scalapb.json.JsonFormat
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

import scala.io.Source

class SimplifiedTweetParserTest extends FlatSpec with PropertyChecks with Matchers {

  it should "parse simplified JSON" in {
    val json = Source.fromInputStream(getClass.getResourceAsStream("/testing.json")).getLines().mkString
    val tweet = JsonFormat.fromJsonString(json)(SimplifiedTweet)
    tweet.user.get.screenName shouldBe "honzam399"
    tweet.user.get.description shouldBe "Pointy-haired engineer & competitive cyclist."

    tweet.entities.get.media.head.mediaUrl shouldBe "http://pbs.twimg.com/media/Cso-f3PWgAApURT.jpg"
  }

}
