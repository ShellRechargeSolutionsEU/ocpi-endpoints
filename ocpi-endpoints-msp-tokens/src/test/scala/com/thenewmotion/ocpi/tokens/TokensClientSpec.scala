package com.thenewmotion.ocpi.tokens

import java.net.UnknownHostException
import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.util.Timeout
import scala.concurrent.duration.FiniteDuration
import org.specs2.matcher.FutureMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import akka.http.scaladsl.model.ContentTypes._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{\/, \/-}
import com.thenewmotion.ocpi.common.ClientObjectUri
import akka.http.scaladsl.model.StatusCodes.{ClientError => _, _}
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.Tokens._
import org.joda.time.DateTime
import org.specs2.concurrent.ExecutionEnv

class TokensClientSpec(implicit ee: ExecutionEnv) extends Specification with FutureMatchers {

  "Tokens client" should {

    "Retrieve a Token as it is stored in the CPO system" in new TestScope {

      client.getToken(tokenUri, "auth", "123") must beLike[\/[ErrorResp, Token]] {
        case \/-(r) =>
          r.uid === "123"
      }.await
    }

    "Push new/updated Token object to the CPO" in new TestScope {

      client.uploadToken(tokenUri, "auth", testToken) must beLike[\/[ErrorResp, Unit]] {
        case \/-(_) => ok
      }.await
    }

    "Notify the CPO of partial updates to a Token" in new TestScope {

      val patch = TokenPatch(
        valid = Some(false)
      )

      client.updateToken(tokenUri, "auth", patch) must beLike[\/[ErrorResp, Unit]] {
        case \/-(_) => ok
      }.await
    }
  }

  trait TestScope extends Scope {

    implicit val system = ActorSystem()

    implicit val materializer = ActorMaterializer()

    val ourCountryCode = "NL"
    val ourPartyId = "TNM"

    val testToken = Token(
      uid = "123",
      `type` = TokenType.Rfid,
      authId = "A1B2C3",
      visualNumber = None,
      issuer = "NewMotion",
      valid = true,
      whitelist = WhitelistType.Allowed,
      lastUpdated = DateTime.now
    )


    val dataUrl = "http://localhost:8095/cpo/versions/2.1/tokens"

    val tokenUri = ClientObjectUri(
      endpointUri = dataUrl,
      ourCountryCode = ourCountryCode,
      ourPartyId = ourPartyId,
      uid = testToken.uid
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
           |    "last_updated" : "${testToken.lastUpdated}"
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

    lazy val client = new TestTokensClient(requestWithAuth)
  }
}

object GenericRespTypes {
  case class TestData(id: String)

  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  implicit val testDataFormat = jsonFormat1(TestData)
}


class TestTokensClient(reqWithAuthFunc: String => Future[HttpResponse])
  (implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends TokensClient {

  override def requestWithAuth(req: HttpRequest, token: String)(implicit ec: ExecutionContext): Future[HttpResponse] =
    req.uri.toString match { case x => reqWithAuthFunc(x) }

}
