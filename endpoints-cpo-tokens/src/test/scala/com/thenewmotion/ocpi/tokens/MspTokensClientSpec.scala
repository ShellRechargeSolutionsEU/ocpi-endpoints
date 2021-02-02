package com.thenewmotion.ocpi.tokens

import java.net.UnknownHostException
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.StatusCodes.{ClientError => _, _}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse, Uri}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Locations.{EvseUid, LocationId}
import com.thenewmotion.ocpi.msgs.{AuthToken, ErrorResp}
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{Allowed, AuthorizationInfo, LocationReferences, TokenUid}
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import cats.effect.IO
import com.thenewmotion.ocpi.common.IOMatchersExt
import com.thenewmotion.ocpi.msgs.sprayjson.v2_1.protocol._

class MspTokensClientSpec(implicit ee: ExecutionEnv) extends Specification with IOMatchersExt {
  "MSpTokensClient" should {

    "request authorization for a token and get it" in new TestScope {

      client.authorize(theirTokensEndpointUri, AuthToken[Ours]("auth"), tokenId,
        locationReferences = None) must returnValueLike[Either[ErrorResp, AuthorizationInfo]] {
        case Right(r) =>
          r.allowed === Allowed.Allowed
      }
    }

    "request authorization for a token on a specific location" in new TestScope {
      val testLocRefs = LocationReferences(locationId = LocationId("ABCDEF"),
        evseUids = List("evse-123456", "evse-1234567").map(EvseUid(_)))
      client.authorize(theirTokensEndpointUri, AuthToken[Ours]("auth"), tokenId,
        locationReferences = Some(testLocRefs)) must returnValueLike[Either[ErrorResp, AuthorizationInfo]] {
        case Right(r) =>
          r.allowed === Allowed.Allowed
      }
    }


    "request authorization for a token and get it, when endpoint ends with a slash already" in new TestScope {
      val urlWithTrailingSlash = Uri(theirTokensEndpoint + "/")

      client.authorize(urlWithTrailingSlash, AuthToken[Ours]("auth"), tokenId,
        locationReferences = None) must returnValueLike[Either[ErrorResp, AuthorizationInfo]] {
        case Right(r) =>
          r.allowed === Allowed.Allowed
      }
    }
  }

  trait TestScope extends Scope {

    implicit val system: ActorSystem = ActorSystem()

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val http: HttpExt = Http()

    val tokenId = TokenUid("DEADBEEF")

    val theirTokensEndpoint = "http://localhost:8095/msp/versions/2.1/tokens"
    val theirTokensEndpointUri = Uri(theirTokensEndpoint)

    val authorizedResp = HttpResponse(
      OK, entity = HttpEntity(`application/json`,
        s"""
          |{
          |  "status_code": 1000,
          |  "timestamp": "2010-01-01T00:00:00Z",
          |  "data": {
          |    "allowed": "ALLOWED",
          |    "info": {
          |      "language": "nl",
          |      "text": "Ga je gang"
          |    }
          |  }
          |}
         """.stripMargin.getBytes)
    )

    implicit val timeout: Timeout = Timeout(2.seconds)
    val tokenAuthorizeUri = s"$theirTokensEndpoint/$tokenId/authorize"

    def requestWithAuth(uri: String) = uri match {
      case `tokenAuthorizeUri` => IO.pure(authorizedResp)
      case x =>                   IO.raiseError(new UnknownHostException(x.toString))
    }

    lazy val client = new TestMspTokensClient(requestWithAuth)

  }
}

// generalize to testhttpclient?
class TestMspTokensClient(reqWithAuthFunc: String => IO[HttpResponse])
  (implicit httpExt: HttpExt) extends MspTokensClient {

  override def requestWithAuth(http: HttpExt, req: HttpRequest, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    req.uri.toString match {
      case x => reqWithAuthFunc(x).unsafeToFuture()
    }
}
