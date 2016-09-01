package com.thenewmotion.ocpi

import akka.actor.ActorSystem
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import com.thenewmotion.ocpi.common.{OcpiClient, UnknownLinkFormat}
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.{DataResponse, SuccessResponse}
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import spray.http.HttpHeaders.RawHeader
import spray.http._
import scala.language.reflectiveCalls
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, \/, \/-}

class OcpiClientSpec extends Specification with FutureMatchers{

  "extractNextUri()" should {
    "parse out next uri from link header" in new TestScope {
      client.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; rel="next"""") mustEqual
        Some(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50"))

      val mtch = client.extractNextUri("""<https://api.github.com/search/code?q=addClass+user%3Amozilla&page=1>; rel="first",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=34>; rel="last",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15>; rel="next",
                                            |  <https://api.github.com/search/code?q=addClass+user%3Amozilla&page=13>; rel="prev"""")
      mtch mustEqual Some(Uri("""https://api.github.com/search/code?q=addClass+user%3Amozilla&page=15"""))

      client.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; rel="last"""") mustEqual
        None
    }

    "crash on wrongly formatted link headers" in new TestScope{
      client.extractNextUri("""<https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50>; relation="next"""") must
        throwA[UnknownLinkFormat]
    }
  }

  "setPageLimit()" should {
    "set page limit to min(server limit, client limit)" in new TestScope {
      client.setPageLimit(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=150")) mustEqual
        Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=100")
      client.setPageLimit(Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50")) mustEqual
        Uri("https://www.server.com/ocpi/cpo/2.0/cdrs/?offset=5&limit=50")
    }
  }

  "generic client" should {

    "download all paginated data" in new TestScope {

      import GenericRespTypes._
      override val data1response = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, data1String.getBytes),
        List(linkHeader(s"$dataUrl?offset=1&limit=1"),
          RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      override val data2response = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`, data2String.getBytes),
        List(RawHeader("X-Total-Count", "2"),
          RawHeader("X-Limit", "1")
        )
      )

      client.getData(dataUrl, "auth") must beLike[\/[GenericError, GenericResp]]{
        case \/-(r) =>
          r.data.size === 2
          r.data.head.id === "DATA1"
          r.data.tail.head.id === "DATA2"
      }.await
    }

    "return errors for wrong urls" in new TestScope {
      import GenericRespTypes._
      client.getData("http://localhost", "auth") must beLike[\/[GenericError, GenericResp]] {
        case -\/(r) =>
          r must haveClass[GenericErrorInstance]
      }.await
    }
  }

  object GenericRespTypes {
    case class GenericData(id: String)
    sealed trait GenericError
    case class GenericErrorInstance() extends GenericError

    case class GenericResp(
      status_code: Int,
      status_message: Option[String] = None,
      timestamp: com.thenewmotion.time.Imports.DateTime = org.joda.time.DateTime.now(),
      data: List[GenericData]
    ) extends SuccessResponse with DataResponse[GenericResp] {
      type DataItem = GenericData
      override def copyData(dataItems: List[GenericData]): GenericResp = copy(data = dataItems)
    }

    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    implicit val genericDataFormat = jsonFormat1(GenericData)
    implicit val genericRespFormat = jsonFormat4(GenericResp)
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    val dataUrl = "http://localhost:8095/cpo/versions/2.0/somemodule"

    def linkHeader(next: Uri) =
      RawHeader("Link", s"""<$next>; rel="next"""")


    val data1String = s"""
                       |{
                       |  "status_code": 1000,
                       |  "timestamp": "2010-01-01T00:00:00Z",
                       |  "data": [{
                       |    "id": "DATA1"
                       |  }]
                       |}
                       |""".stripMargin

    val data2String = data1String.replace("DATA1","DATA2")

    def data1response: HttpResponse = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`,
      data1String.getBytes), List(linkHeader(s"$dataUrl?offset=1&limit=1"),
        RawHeader("X-Total-Count", "2"),
        RawHeader("X-Limit", "1")
      )
    )
    def data2response: HttpResponse = HttpResponse(StatusCodes.OK, HttpEntity(ContentTypes.`application/json`,
      data2String.getBytes), List(RawHeader("X-Total-Count", "2"), RawHeader("X-Limit", "1")
      )
    )




    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    lazy val client = new OcpiClient{

      import scala.concurrent.ExecutionContext.Implicits.global
      import spray.client.pipelining._
      import spray.httpx.SprayJsonSupport._
      import GenericRespTypes._


      val urlPattern = s"$dataUrl\\?offset=([0-9]+)&limit=[0-9]+".r

      override def sendAndReceive = (req:HttpRequest) => req.uri.toString match {
        case urlPattern(offset) if offset == "0" => Future.successful(data1response)
        case urlPattern(offset) if offset == "1" => Future.successful(data2response)
        case urlPattern(offset) => println(s"got offset $offset. "); throw new RuntimeException()
        case x => println(s"got request url |$x|. "); throw new RuntimeException()
      }

      def getData(uri: Uri, auth: String)(implicit ec: ExecutionContext): Future[GenericError \/ GenericResp] =
        traversePaginatedResource(uri, auth, GenericErrorInstance())(unmarshal[GenericResp])
    }

  }
}
