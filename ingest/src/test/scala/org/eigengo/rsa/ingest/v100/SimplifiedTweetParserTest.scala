package org.eigengo.rsa.ingest.v100

import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source
import scala.util.Success

class SimplifiedTweetParserTest extends FlatSpec with PropertyChecks with Matchers {

  it should "parse simplified JSON" in {
    val json = Source.fromInputStream(getClass.getResourceAsStream("/testing.json")).getLines().mkString
    val Success(simplifiedTweet) = SimplifiedTweetFormat.parse(json)
    simplifiedTweet.handle shouldBe "honzam399"
    simplifiedTweet.mediaUrls.length shouldBe 1
    simplifiedTweet.mediaUrls should contain ("http://pbs.twimg.com/media/Cso-f3PWgAApURT.jpg")
  }

}
