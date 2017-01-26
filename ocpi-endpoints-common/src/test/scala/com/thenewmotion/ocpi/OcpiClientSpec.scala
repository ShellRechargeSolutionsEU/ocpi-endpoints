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
import com.thenewmotion.ocpi.common.{OcpiClient, UnexpectedResponseException}
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{ErrorResp, SuccessWithDataResp}
import org.specs2.concurrent.ExecutionEnv
import akka.http.scaladsl.model.StatusCodes._
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericClientFailure
import akka.http.scaladsl.client.RequestBuilding._

class OcpiClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  case class TestData(id: String)
  implicit val testDataFormat = jsonFormat1(TestData)

  "single request" should {
    "unmarshal success response" in new TestScope {
      client.singleRequest[SuccessWithDataResp[TestData]](
        Get(singleRequestOKUrl), "auth")  must beLike[\/[ErrorResp, SuccessWithDataResp[TestData]]] {
        case \/-(r) => r.data.id mustEqual "monkey"
      }.await
    }

    "unmarshal error response" in new TestScope {
      client.singleRequest[SuccessWithDataResp[TestData]](
        Get(singleRequestErrUrl), "auth")  must beLike[\/[ErrorResp, SuccessWithDataResp[TestData]]] {
        case -\/(err) =>
          err.statusCode mustEqual GenericClientFailure
          err.statusMessage must beSome("something went horribly wrong...")
      }.await
    }
  }

  "traverse paginated resource" should {
    "download subsequent pages" in new TestScope {
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

      client.traversePaginatedResource[TestData](dataUrl, "auth", limit = 1) must beLike[\/[ErrorResp, Iterable[TestData]]] {
        case \/-(r) =>
          r.size === 2
          r.head.id === "DATA1"
          r.tail.head.id === "DATA2"
      }.await
    }

    "throw an exception when server returns a none successful error code" in new TestScope {
      client.traversePaginatedResource[TestData](notFoundUrl, "auth", limit = 1) must throwA[UnexpectedResponseException].await
    }

    "handle empty list of data items" in new TestScope {
      client.traversePaginatedResource[TestData](emptyUrl, "auth", limit = 1) must beLike[\/[ErrorResp, Iterable[TestData]]] {
        case \/-(loc) => loc mustEqual Nil
      }.await
    }

    "handle OCPI error codes" in new TestScope {
      client.traversePaginatedResource[TestData](ocpiErrorUrl, "auth", limit = 1) must beLike[\/[ErrorResp, Iterable[TestData]]] {
        case -\/(err) =>
          err.statusCode mustEqual GenericClientFailure
          err.statusMessage must beSome("something went horribly wrong...")
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

      client.traversePaginatedResource[TestData](
        dataUrl, "auth", Map("date_from" -> "2016-11-23T08:04:01Z"), limit = 1) must beLike[\/[ErrorResp, Iterable[TestData]]] {
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

    def successResponse = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": {
           |    "id": "monkey"
           |  }
           |}
           |""".stripMargin.getBytes)
    )

    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    val wrongJsonUrl = s"$dataUrl/wrongjson"
    val notFoundUrl = s"$dataUrl/notfound"
    val emptyUrl = s"$dataUrl/empty"
    val ocpiErrorUrl = s"$dataUrl/ocpierror"
    val singleRequestOKUrl = s"$dataUrl/animals-ok"
    val singleRequestErrUrl = s"$dataUrl/animals-err"

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
      case `singleRequestOKUrl` => Future.successful(successResponse)
      case `singleRequestErrUrl` => Future.successful(ocpiErrorResp)
      case x =>
        println(s"got request url |$x|. ")
        Future.failed(new UnknownHostException("www.ooopsie.com"))
    }

    lazy val client = new TestOcpiClient(requestWithAuth)
  }
}


class TestOcpiClient(reqWithAuthFunc: String => Future[HttpResponse])
  (implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {

  override def requestWithAuth(req: HttpRequest, token: String)(implicit ec: ExecutionContext): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x) }
}
