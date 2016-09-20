package org.eigengo.rsa.ingest.v100

import org.json4s.JsonAST.JString
import org.json4s.JsonInput
import org.json4s.jackson.JsonMethods

import scala.util.Try

trait SimplifiedTweetFormat {

  def parse(json: JsonInput): Try[SimplifiedTweet] = {
    Try {
      val jTweet = JsonMethods.parse(json)
      val JString(handle) = jTweet \ "user" \ "screen_name"
      val mediaUrls = (jTweet \ "entities" \ "media").filterField { case (f, _) ⇒ f == "media_url" }.map { case (_, JString(url)) ⇒ url }

      SimplifiedTweet(handle = handle, mediaUrls = mediaUrls)
    }
  }

}

object SimplifiedTweetFormat extends SimplifiedTweetFormat
