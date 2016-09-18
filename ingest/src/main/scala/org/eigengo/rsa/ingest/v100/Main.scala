package org.eigengo.rsa.ingest.v100

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load("ingest.conf").resolve()

  //Get your credentials from https://apps.twitter.com and replace the values below
  private val consumerKey = "sss"
  private val consumerSecret = "yyy"
  private val accessToken = "ssss"
  private val accessTokenSecret = "ssss"
  private val url = "https://stream.twitter.com/1.1/statuses/filter.json"

  implicit val system = ActorSystem("Ingest", config)
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  private val consumer = new DefaultConsumerService(system.dispatcher)

  //Filter tweets by a term "london"
  val body = "track=%23ReactiveSummit"
  val source = Uri(url)

  //Create Oauth 1a header
  val oauthHeader: Future[String] = consumer.createOauthenticatedRequest(
    KoauthRequest(
      method = "POST",
      url = url,
      authorizationHeader = None,
      body = Some(body)
    ),
    consumerKey,
    consumerSecret,
    accessToken,
    accessTokenSecret
  ).map(_.header)

  oauthHeader.onComplete {
    case Success(header) =>
      val httpHeaders: List[HttpHeader] = List(
        HttpHeader.parse("Authorization", header) match {
          case ParsingResult.Ok(h, _) => Some(h)
          case _ => None
        },
        HttpHeader.parse("Accept", "*/*") match {
          case ParsingResult.Ok(h, _) => Some(h)
          case _ => None
        }
      ).flatten
      val httpRequest: HttpRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = source,
        headers = httpHeaders,
        entity = HttpEntity(contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), string = body)
      )
      val request = Http().singleRequest(httpRequest)
      request.flatMap { response =>
        if (response.status.intValue() != 200) {
          println("1:(" + response.entity.dataBytes.runForeach(_.utf8String))
          Future(Unit)
        } else {
          response.entity.dataBytes
            .scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
            .filter(_.contains("\r\n"))
            //.map(json => Try(parse(json).extract[Tweet]))
            .runForeach(println)
        }
      }
    case Failure(failure) => println("2:(" + failure.getMessage)
  }

}
