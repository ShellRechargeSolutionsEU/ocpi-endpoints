package com.thenewmotion.ocpi.common

import java.net.UnknownHostException

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericClientFailure
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.{AuthToken, ErrorResp, SuccessWithDataResp}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

class OcpiClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  case class TestData(id: String)
  implicit val testDataFormat = jsonFormat1(TestData)

  "single request" should {
    "unmarshal success response" in new TestScope {
      client.singleRequest[SuccessWithDataResp[TestData]](
        Get(singleRequestOKUrl), AuthToken[Ours]("auth"))  must beLike[\/[ErrorResp, SuccessWithDataResp[TestData]]] {
        case \/-(r) => r.data.id mustEqual "monkey"
      }.await
    }

    "unmarshal error response" in new TestScope {
      client.singleRequest[SuccessWithDataResp[TestData]](
        Get(singleRequestErrUrl), AuthToken[Ours]("auth"))  must beLike[\/[ErrorResp, SuccessWithDataResp[TestData]]] {
        case -\/(err) =>
          err.statusCode mustEqual GenericClientFailure
          err.statusMessage must beSome("something went horribly wrong...")
      }.await
    }
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    implicit val http = Http()

    val dataUrl = "http://localhost:8095/cpo/versions/2.0/somemodule"

    def notFoundResp = HttpResponse(NotFound)

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
      case urlPattern(offset) => println(s"got offset $offset. "); Future.failed(throw new RuntimeException())
      case `notFoundUrlWithParams` => Future.successful(notFoundResp)
      case `ocpiErrorUrlWithParams` => Future.successful(ocpiErrorResp)
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
  (implicit http: HttpExt) extends OcpiClient {

  override def requestWithAuth(http: HttpExt, req: HttpRequest, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x) }
}
