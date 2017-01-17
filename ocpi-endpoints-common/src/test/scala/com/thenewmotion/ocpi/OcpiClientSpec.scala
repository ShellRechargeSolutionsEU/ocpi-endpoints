package com.thenewmotion.ocpi

import java.net.UnknownHostException
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.{Link, LinkParams, RawHeader}
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import akka.http.scaladsl.model.ContentTypes._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.thenewmotion.ocpi.common.{ClientError, OcpiClient}
import akka.http.scaladsl.model.StatusCodes.{ClientError => _, _}
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import org.specs2.concurrent.ExecutionEnv

class OcpiClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  import GenericRespTypes._

  "generic client" should {

    "download all paginated data" in new TestScope {
      override val firstPageResp = HttpResponse(
        OK, entity = HttpEntity(`application/json`, firstPageBody.getBytes),
        headers = List(
          Link(Uri(s"$dataUrl?offset=1&limit=1"), LinkParams.next),
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      override val lastPageResp = HttpResponse(
        OK, entity = HttpEntity(`application/json`, lastPageBody.getBytes),
        headers = List(
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      client.getData(dataUrl, "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
        case \/-(r) =>
          r.size === 2
          r.head.id === "DATA1"
          r.tail.head.id === "DATA2"
      }.await
    }

    "return an error for wrong urls" in new TestScope {
      client.getData("http://localhost:8095", "auth") must  beLike[\/[ClientError, Iterable[TestData]]] {
        case -\/(err) => err must haveClass[ClientError.ConnectionFailed]
      }.await
    }

    "handle JSON that can't be unmarshalled" in new TestScope {
      client.getData(s"$wrongJsonUrl", "auth") must  beLike[\/[ClientError, Iterable[TestData]]] {
        case -\/(err) => err must haveClass[ClientError.UnmarshallingFailed]
      }.await
    }

    "translate exception from 404 result to left disjuntion client error" in new TestScope {
      client.getData(s"$notFoundUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
        case -\/(err) => err must haveClass[ClientError.NotFound]
      }.await
    }

    "handle empty list of data items" in new TestScope {
      client.getData(s"$emptyUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
        case \/-(loc) => loc mustEqual Nil
      }.await
    }

    "handle OCPI error codes" in new TestScope {
      client.getData(s"$ocpiErrorUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
        case -\/(err) => err mustEqual ClientError.OcpiClientError(Some("something went horribly wrong..."))
      }.await
    }

    "address extra params with the query" in new TestScope {

      override val firstPageResp = HttpResponse(
        OK, entity = HttpEntity(`application/json`, firstPageBody.getBytes),
        headers = List(
          RawHeader("X-Total-Count", "1"),
          RawHeader("X-Limit", "1")
        )
      )

      client.getData(dataUrl, "auth", Map("date_from" -> "2016-11-23T08:04:01Z")) must beLike[\/[ClientError, Iterable[TestData]]] {
        case \/-(r) =>
          r.size === 1
          r.head.id === "DATA1"
      }.await
    }
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    val dataUrl = "http://localhost:8095/cpo/versions/2.0/somemodule"

    val firstPageBody = s"""
                           |{
                           |  "status_code": 1000,
                           |  "timestamp": "2010-01-01T00:00:00Z",
                           |  "data": [{
                           |    "id": "DATA1"
                           |  }]
                           |}
                           |""".stripMargin

    val lastPageBody = firstPageBody.replace("DATA1","DATA2")

    def firstPageResp: HttpResponse = HttpResponse(
      OK, entity = HttpEntity(`application/json`, firstPageBody.getBytes),
      headers = List(
        Link(Uri(s"$dataUrl?offset=1&limit=1"), LinkParams.next),
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1")
      )
    )
    def lastPageResp: HttpResponse = HttpResponse(
      OK, entity = HttpEntity(`application/json`, lastPageBody.getBytes),
      headers = List(
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1"))
    )

    def wrongJsonResp: HttpResponse = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
         |{
         |  "status_code": 1000,
         |  "timestamp": "2010-01-01T00:00:00Z",
         |  "data": [{
         |    "id": "DATA1",
         |  }]
         |}
         |""".stripMargin.getBytes)
    )

    def notFoundResp = HttpResponse(NotFound)

    def emptyResp = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": []
           |}
           |""".stripMargin.getBytes)
    )

    def ocpiErrorResp = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 2000,
           |  "status_message": "something went horribly wrong...",
           |  "timestamp": "2010-01-01T00:00:00Z"
           |}
           |""".stripMargin.getBytes)
    )

    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    val wrongJsonUrl = s"$dataUrl/wrongjson"
    val notFoundUrl = s"$dataUrl/notfound"
    val emptyUrl = s"$dataUrl/empty"
    val ocpiErrorUrl = s"$dataUrl/ocpierror"

    val urlPattern = s"$dataUrl\\?offset=([0-9]+)&limit=[0-9]+".r
    val wrongJsonUrlWithParams = s"$wrongJsonUrl?offset=0&limit=1"
    val notFoundUrlWithParams = s"$notFoundUrl?offset=0&limit=1"
    val emptyUrlWithParams = s"$emptyUrl?offset=0&limit=1"
    val ocpiErrorUrlWithParams = s"$ocpiErrorUrl?offset=0&limit=1"
    val urlWithExtraParams = s"$dataUrl?offset=0&limit=1&date_from=2016-11-23T08:04:01Z"

    def requestWithAuth(uri: String) = uri match {
      case urlPattern(offset) if offset == "0" => Future.successful(firstPageResp)
      case urlPattern(offset) if offset == "1" => Future.successful(lastPageResp)
      case urlPattern(offset) => println(s"got offset $offset. "); Future.failed(throw new RuntimeException())
      case `wrongJsonUrlWithParams` => println(s"serving wrong JSON");Future.successful(wrongJsonResp)
      case `notFoundUrlWithParams` => Future.successful(notFoundResp)
      case `emptyUrlWithParams` => Future.successful(emptyResp)
      case `ocpiErrorUrlWithParams` => Future.successful(ocpiErrorResp)
      case `urlWithExtraParams` => Future.successful(firstPageResp)
      case x =>
        println(s"got request url |$x|. ")
        Future.failed(new UnknownHostException("www.ooopsie.com"))
    }

    lazy val client = new TestOcpiClient(requestWithAuth)
  }
}

object GenericRespTypes {
  case class TestData(id: String)

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  implicit val testDataFormat = jsonFormat1(TestData)
}


class TestOcpiClient(reqWithAuthFunc: String => Future[HttpResponse])
  (implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {

  import GenericRespTypes._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  override def requestWithAuth(req: HttpRequest, token: String)(implicit ec: ExecutionContext): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x) }

  def getData(uri: Uri, auth: String, params: Map[String, String] = Map.empty)
             (implicit ec: ExecutionContext): Future[ClientError \/ Iterable[TestData]] =
    traversePaginatedResource(uri, auth, params, limit = 1)(res => Unmarshal(res.entity).to[Page[TestData]])
}
