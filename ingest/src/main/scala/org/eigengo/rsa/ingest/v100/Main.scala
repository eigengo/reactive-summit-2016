package org.eigengo.rsa.ingest.v100

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Accept, Authorization, GenericHttpCredentials}
import akka.stream.ActorMaterializer
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import com.typesafe.config.ConfigFactory

object Main extends App {
  val config = ConfigFactory.load("ingest.conf").resolve()

  val consumerKey = config.getString("app.twitter.consumerKey")
  val consumerSecret = config.getString("app.twitter.consumerSecret")
  val accessToken = config.getString("app.twitter.accessToken")
  val accessTokenSecret = config.getString("app.twitter.accessTokenSecret")
  val url = "https://stream.twitter.com/1.1/statuses/filter.json"

  implicit val system = ActorSystem("Ingest", config)
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  def run(source: Uri, body: String)(oauthHeader: String): Unit = {
    val headers = List(Accept(MediaRanges.`*/*`), Authorization(GenericHttpCredentials("https", oauthHeader)))
    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = source,
      headers = headers,
      entity = HttpEntity(contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), string = body)
    )
    val request = Http().singleRequest(httpRequest)
    request.foreach { response ⇒
      if (response.status.intValue() == 200) {
        response.entity.dataBytes
          .scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
          .filter(_.contains("\r\n"))
          //.map(json => Try(parse(json).extract[Tweet]))
          .runForeach(println)
      }
    }
  }

  val consumer = new DefaultConsumerService(system.dispatcher)

  //val body = "track=%23ReactiveSummit"
  val body = "track=New%20York"
  val source = Uri(url)

  val koauthRequest = KoauthRequest(method = "POST", url = url, authorizationHeader = None, body = Some(body))
  consumer.createOauthenticatedRequest(koauthRequest, consumerKey, consumerSecret, accessToken, accessTokenSecret)
    .map(_.header)
    .foreach(run(source, body))

//  //Create Oauth 1a header
//  val oauthHeader: Future[String] = consumer.createOauthenticatedRequest(
//    KoauthRequest(
//      method = "POST",
//      url = url,
//      authorizationHeader = None,
//      body = Some(body)
//    ),
//    consumerKey,
//    consumerSecret,
//    accessToken,
//    accessTokenSecret
//  ).map(_.header)
//
//  oauthHeader.onComplete {
//    case Success(header) =>
//      val httpHeaders: List[HttpHeader] = List(
//        HttpHeader.parse("Authorization", header) match {
//          case ParsingResult.Ok(h, _) => Some(h)
//          case _ => None
//        },
//        HttpHeader.parse("Accept", "*/*") match {
//          case ParsingResult.Ok(h, _) => Some(h)
//          case _ => None
//        }
//      ).flatten
//      val httpRequest: HttpRequest = HttpRequest(
//        method = HttpMethods.POST,
//        uri = source,
//        headers = httpHeaders,
//        entity = HttpEntity(contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), string = body)
//      )
//      val request = Http().singleRequest(httpRequest)
//      request.flatMap { response =>
//        if (response.status.intValue() != 200) {
//          println("1:(" + response.entity.dataBytes.runForeach(_.utf8String))
//          Future(Unit)
//        } else {
//          response.entity.dataBytes
//            .scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
//            .filter(_.contains("\r\n"))
//            //.map(json => Try(parse(json).extract[Tweet]))
//            .runForeach(println)
//        }
//      }
//    case Failure(failure) => println("2:(" + failure.getMessage)
//  }

}
