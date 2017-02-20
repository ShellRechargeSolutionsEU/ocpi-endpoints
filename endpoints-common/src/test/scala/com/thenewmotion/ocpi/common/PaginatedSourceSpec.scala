package com.thenewmotion.ocpi.common

import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, Link, LinkParams, RawHeader}
import akka.stream.ActorMaterializer
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import com.thenewmotion.ocpi.msgs.{ErrorResp, OcpiResponse, OcpiStatusCode, SuccessWithDataResp}
import org.mockito.Matchers
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.json._

import scala.concurrent.Future

class PaginatedSourceSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  case class TestData(id: String)
  implicit val testDataFormat = jsonFormat1(TestData)

  "PaginatedSource" should {
    "Follow the link to the next page" in new TestScope {

      implicit val ac = system

      mockRequest(page1Req,
        SuccessWithDataResp(
          OcpiStatusCode.GenericSuccess,
          data = List(TestData("DATA1"), TestData("DATA2"))
        ),
        headers = List(
          Link(Uri(s"$dataUrl?offset=2&limit=2"), LinkParams.next),
          RawHeader("X-Total-Count", "4"),
          RawHeader("X-Limit", "2")
        )
      )

      mockRequest(page2Req,
        SuccessWithDataResp(
          OcpiStatusCode.GenericSuccess,
          data = List(TestData("DATA3"), TestData("DATA4"))
        ),
        headers = List(
          RawHeader("X-Total-Count", "4"),
          RawHeader("X-Limit", "2")
        )
      )

      val probe = TestProbe()

      PaginatedSource[TestData](http, dataUrl, "auth", limit = 2)
        .runWith(TestSink.probe[TestData])
        .request(5)
        .expectNext(TestData("DATA1"), TestData("DATA2"), TestData("DATA3"), TestData("DATA4"))
        .expectComplete()
    }

    "Handle OCPI error" in new TestScope {

      implicit val ac = system

      mockRequest(page1Req,
        SuccessWithDataResp(
          OcpiStatusCode.GenericSuccess,
          data = List(TestData("DATA1"), TestData("DATA2"))
        ),
        headers = List(
          Link(Uri(s"$dataUrl?offset=2&limit=2"), LinkParams.next),
          RawHeader("X-Total-Count", "4"),
          RawHeader("X-Limit", "2")
        )
      )

      mockRequest(page2Req,
        ErrorResp(
          OcpiStatusCode.GenericServerFailure,
          statusMessage = Some("something went horribly wrong...")
        )
      )

      val probe = TestProbe()

      PaginatedSource[TestData](http, dataUrl, "auth", limit = 2)
        .runWith(TestSink.probe[TestData])
        .request(5)
        .expectNext(TestData("DATA1"), TestData("DATA2"))
        .expectError()
    }
  }

  trait TestScope extends Scope {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val dataUrl = "http://localhost:8095/cpo/versions/2.0/somemodule"

    val http = mock[HttpExt]

    val baseReq = Get(Uri(dataUrl)).addCredentials(GenericHttpCredentials("Token", "auth", Map()))

    val page1Req = baseReq.withUri(baseReq.uri.withQuery(Query(Map(
      "offset" -> "0",
      "limit"  -> "2"
    ))))

    val page2Req = baseReq.withUri(baseReq.uri.withQuery(Query(Map(
      "offset" -> "2",
      "limit"  -> "2"
    ))))

    def mockRequest[R <: OcpiResponse[_] : JsonFormat](req: HttpRequest, response: R, headers: List[HttpHeader] = List.empty) =
      http.singleRequest(Matchers.eq(req), any, any, any)(any) returns Future.successful(HttpResponse(
        OK, entity = HttpEntity(`application/json`, CompactPrinter(response.toJson).getBytes),
        headers = headers
      ))
  }

}
