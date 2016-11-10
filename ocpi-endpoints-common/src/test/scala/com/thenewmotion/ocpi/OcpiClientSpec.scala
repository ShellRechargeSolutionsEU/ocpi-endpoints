package com.thenewmotion.ocpi

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import com.thenewmotion.ocpi.common.{ClientError, OcpiClient}
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.Page
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http._
import HttpHeaders._
import StatusCodes._
import ContentTypes._
import org.specs2.concurrent.ExecutionEnv
import scala.language.reflectiveCalls
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

class OcpiClientSpec extends Specification with FutureMatchers {

  import GenericRespTypes._

  "generic client" should {

    "download all paginated data" >>  { implicit ee: ExecutionEnv =>
      new TestScope {

        override val firstPageResp = HttpResponse(
          OK, HttpEntity(`application/json`, firstPageBody.getBytes),
          List(
            Link(Uri(s"$dataUrl?offset=1&limit=1"), Link.next),
            RawHeader("X-Total-Count", "2"),
            RawHeader("X-Limit", "1")
          )
        )

        override val lastPageResp = HttpResponse(
          OK, HttpEntity(`application/json`, lastPageBody.getBytes),
          List(
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
    }

    "return an error for wrong urls" >>  { implicit ee: ExecutionEnv =>
      new TestScope {
        client.getData("http://localhost:8095", "auth") must  beLike[\/[ClientError, Iterable[TestData]]] {
          case -\/(err) => err must haveClass[ClientError.ConnectionFailed]
        }.await
      }
    }

    "handle JSON that can't be unmarshalled" >> {implicit ee: ExecutionEnv =>
      new TestScope {
        client.getData(s"$wrongJsonUrl", "auth") must  beLike[\/[ClientError, Iterable[TestData]]] {
          case -\/(err) => err must haveClass[ClientError.UnmarshallingFailed]
        }.await
      }
    }

    "translate exception from 404 result to left disjuntion client error" >> {implicit ee: ExecutionEnv =>
      new TestScope {
        client.getData(s"$notFoundUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
          case -\/(err) => err must haveClass[ClientError.NotFound]
        }.await
      }
    }

    "handle empty list of data items" >> {implicit ee: ExecutionEnv =>
      new TestScope {
        client.getData(s"$emptyUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
          case \/-(loc) => loc mustEqual Nil
        }.await
      }
    }

    "handle OCPI error codes" >> {implicit ee: ExecutionEnv =>
      new TestScope {
        client.getData(s"$ocpiErrorUrl", "auth") must beLike[\/[ClientError, Iterable[TestData]]] {
          case -\/(err) => err mustEqual ClientError.OcpiClientError(Some("something went horribly wrong..."))
        }.await
      }
    }
  }

  object GenericRespTypes {
    case class TestData(id: String)

    import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
    implicit val testDataFormat = jsonFormat1(TestData)
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

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
      OK, HttpEntity(`application/json`,
        firstPageBody.getBytes), List(
        Link(Uri(s"$dataUrl?offset=1&limit=1"), Link.next),
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1")
      )
    )
    def lastPageResp: HttpResponse = HttpResponse(
      OK, HttpEntity(`application/json`, lastPageBody.getBytes),
      List(
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1"))
    )

    def wrongJsonResp: HttpResponse = HttpResponse(
      OK, HttpEntity(`application/json`,
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
      OK, HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": []
           |}
           |""".stripMargin.getBytes)
    )

    def ocpiErrorResp = HttpResponse(
      OK, HttpEntity(`application/json`,
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

    lazy val client = new OcpiClient {

      import scala.concurrent.ExecutionContext.Implicits.global
      import spray.client.pipelining._
      import spray.httpx.SprayJsonSupport._
      import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

      import GenericRespTypes._


      val urlPattern = s"$dataUrl\\?offset=([0-9]+)&limit=[0-9]+".r
      val wrongJsonUrlWithParams = s"$wrongJsonUrl?offset=0&limit=100"
      val notFoundUrlWithParams = s"$notFoundUrl?offset=0&limit=100"
      val emptyUrlWithParams = s"$emptyUrl?offset=0&limit=100"
      val ocpiErrorUrlWithParams = s"$ocpiErrorUrl?offset=0&limit=100"


      override def sendAndReceive = (req:HttpRequest) => req.uri.toString match {
        case urlPattern(offset) if offset == "0" => Future.successful(firstPageResp)
        case urlPattern(offset) if offset == "1" => Future.successful(lastPageResp)
        case urlPattern(offset) => println(s"got offset $offset. "); Future.failed(throw new RuntimeException())
        case `wrongJsonUrlWithParams` => println(s"serving wrong JSON");Future.successful(wrongJsonResp)
        case `notFoundUrlWithParams` => Future.successful(notFoundResp)
        case `emptyUrlWithParams` => Future.successful(emptyResp)
        case `ocpiErrorUrlWithParams` => Future.successful(ocpiErrorResp)
        case x =>
          println(s"got request url |$x|. ")
          Future.failed(throw new spray.can.Http.ConnectionAttemptFailedException("localhost", 8095))
      }

      def getData(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[ClientError \/ Iterable[TestData]] =
        traversePaginatedResource(uri, auth)(unmarshal[Page[TestData]])
    }

  }
}