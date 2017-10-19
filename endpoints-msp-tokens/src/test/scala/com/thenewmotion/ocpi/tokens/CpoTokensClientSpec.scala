package com.thenewmotion.ocpi
package tokens

import java.net.UnknownHostException
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration

import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import akka.http.scaladsl.model.ContentTypes._
import scala.concurrent.{ExecutionContext, Future}

import com.thenewmotion.ocpi.common.ClientObjectUri
import akka.http.scaladsl.model.StatusCodes.{ClientError => _, _}
import akka.stream.{ActorMaterializer, Materializer}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.{AuthToken, ErrorResp}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import org.specs2.concurrent.ExecutionEnv
import com.thenewmotion.ocpi.ZonedDateTimeParser._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.thenewmotion.ocpi.msgs.v2_1.TokensJsonProtocol._
import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._

class CpoTokensClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  "CPO Tokens client" should {

    "Retrieve a Token as it is stored in the CPO system" in new TestScope {

      client.getToken(tokenUri, AuthToken[Ours]("auth"), TokenUid("123")) must beLike[Either[ErrorResp, Token]] {
        case Right(r) =>
          r.uid === TokenUid("123")
      }.await
    }

    "Push new/updated Token object to the CPO" in new TestScope {

      client.uploadToken(tokenUri, AuthToken[Ours]("auth"), testToken) must beLike[Either[ErrorResp, Unit]] {
        case Right(_) => ok
      }.await
    }

    "Notify the CPO of partial updates to a Token" in new TestScope {

      val patch = TokenPatch(
        valid = Some(false)
      )

      client.updateToken(tokenUri, AuthToken[Ours]("auth"), patch) must beLike[Either[ErrorResp, Unit]] {
        case Right(_) => ok
      }.await
    }
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    implicit val http = Http()

    val ourCountryCode = "NL"
    val ourPartyId = "TNM"

    val testToken = Token(
      uid = TokenUid("123"),
      `type` = TokenType.Rfid,
      authId = AuthId("A1B2C3"),
      visualNumber = None,
      issuer = "NewMotion",
      valid = true,
      whitelist = WhitelistType.Allowed,
      lastUpdated = ZonedDateTime.now
    )


    val dataUrl = "http://localhost:8095/cpo/versions/2.1/tokens"

    val tokenUri = ClientObjectUri(
      endpointUri = dataUrl,
      ourCountryCode = ourCountryCode,
      ourPartyId = ourPartyId,
      ids = testToken.uid.value
    )

    def successResp = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
           |{
           |  "status_code": 1000,
           |  "timestamp": "2010-01-01T00:00:00Z",
           |  "data": {
           |    "uid": "${testToken.uid}",
           |    "type": "RFID",
           |    "auth_id": "${testToken.authId}",
           |    "issuer": "${testToken.issuer}",
           |    "valid": ${testToken.valid},
           |    "whitelist": "${testToken.whitelist}",
           |    "last_updated" : "${format(testToken.lastUpdated)}"
           |  }
           |}
           |""".stripMargin.getBytes)
    )

    implicit val timeout: Timeout = Timeout(FiniteDuration(20, "seconds"))

    val tokenUrl = s"$dataUrl/$ourCountryCode/$ourPartyId/${testToken.uid}"

    def requestWithAuth(uri: String) = uri match {
      case `tokenUrl` => Future.successful(successResp)
      case x =>         Future.failed(new UnknownHostException(x.toString))
    }

    lazy val client = new TestCpoTokensClient(requestWithAuth)
  }
}

object GenericRespTypes {
  case class TestData(id: String)

  import com.thenewmotion.ocpi.msgs.v2_1.DefaultJsonProtocol._
  implicit val testDataFormat = jsonFormat1(TestData)
}


class TestCpoTokensClient(reqWithAuthFunc: String => Future[HttpResponse])
  (implicit httpExt: HttpExt) extends CpoTokensClient {

  override def requestWithAuth(http: HttpExt, req: HttpRequest, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x) }

}
