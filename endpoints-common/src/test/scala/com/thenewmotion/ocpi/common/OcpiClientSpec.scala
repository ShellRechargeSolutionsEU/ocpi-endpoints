package com.thenewmotion.ocpi.common

import java.net.UnknownHostException
import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import cats.effect.IO
import com.thenewmotion.ocpi.msgs.OcpiStatusCode.GenericClientFailure
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.{AuthToken, ErrorResp, SuccessResp}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class OcpiClientSpec(implicit ee: ExecutionEnv) extends Specification with IOMatchersExt {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

  case class TestData(id: String)
  implicit val testDataFormat = jsonFormat1(TestData)

  "single request" should {
    "unmarshal success response with data" in new TestScope {
      client.singleRequest[TestData](
        Get(singleRequestOKUrl), AuthToken[Ours]("auth"))  must
          returnValueLike[Either[ErrorResp, SuccessResp[TestData]]] {
            case Right(r) => r.data.id mustEqual "monkey"
          }
    }

    "unmarshal success response without data" in new TestScope {
      client.singleRequest[Unit](
        Get(singleRequestOKUrl), AuthToken[Ours]("auth"))  must
          returnValueLike[Either[ErrorResp, SuccessResp[Unit]]] {
            case Right(r) => r.data.mustEqual(())
          }
    }

    "unmarshal error response" in new TestScope {
      client.singleRequest[TestData](
        Get(singleRequestErrUrl), AuthToken[Ours]("auth"))  must
          returnValueLike[Either[ErrorResp, SuccessResp[TestData]]] {
            case Left(err) =>
              err.statusCode mustEqual GenericClientFailure
              err.statusMessage must beSome("something went horribly wrong...")
          }
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

    val singleRequestOKUrl = s"$dataUrl/animals-ok"
    val singleRequestErrUrl = s"$dataUrl/animals-err"

    val urlPattern = s"$dataUrl\\?offset=([0-9]+)&limit=[0-9]+".r
    val urlWithExtraParams = s"$dataUrl?offset=0&limit=1&date_from=2016-11-23T08:04:01Z"

    def requestWithAuth(uri: String) = uri match {
      case urlPattern(offset) => println(s"got offset $offset. "); IO.raiseError(throw new RuntimeException())
      case `singleRequestOKUrl` => IO.pure(successResponse)
      case `singleRequestErrUrl` => IO.pure(ocpiErrorResp)
      case x =>
        println(s"got request url |$x|. ")
        IO.raiseError(new UnknownHostException("www.ooopsie.com"))
    }

    lazy val client = new TestOcpiClient(requestWithAuth)
  }
}

class TestOcpiClient(reqWithAuthFunc: String => IO[HttpResponse])
  (implicit http: HttpExt) extends OcpiClient {

  override def requestWithAuth(http: HttpExt, req: HttpRequest, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x).unsafeToFuture() }
}
