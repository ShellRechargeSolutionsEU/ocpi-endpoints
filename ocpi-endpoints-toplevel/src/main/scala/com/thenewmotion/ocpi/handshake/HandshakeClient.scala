package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{SuccessWithDataResp, Url}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}

class HandshakeClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def getTheirVersions(uri: Uri, token: String)(implicit ec: ExecutionContext): Future[HandshakeError \/ SuccessWithDataResp[List[Version]]] = {
    val pipeline = request(token) ~> unmarshal[SuccessWithDataResp[List[Version]]]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case Success(versions) => \/-(versions)
      case Failure(t) =>
        logger.error(s"Could not retrieve the versions information from $uri with token $token. Reason: ${t.getLocalizedMessage}", t)
        -\/(VersionsRetrievalFailed)
    }
  }

  def getTheirVersionDetails(uri: Uri, token: String)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ SuccessWithDataResp[VersionDetails]] = {
    val pipeline = request(token) ~> unmarshal[SuccessWithDataResp[VersionDetails]]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case Success(versionDet) => \/-(versionDet)
      case Failure(t) =>
        logger.error(s"Could not retrieve the version details from $uri with token $token. Reason: ${t.getLocalizedMessage}", t)
        -\/(VersionDetailsRetrievalFailed)
    }
  }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: String, credToConnectToUs: Creds)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ SuccessWithDataResp[Creds]] = {
    val pipeline = request(tokenToConnectToThem) ~> unmarshal[SuccessWithDataResp[Creds]]
    val resp = pipeline(Post(theirCredUrl, credToConnectToUs))
    bimap(resp) {
      case Success(theirCreds) => \/-(theirCreds)
      case Failure(t) =>
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: ${t.getLocalizedMessage}", t )
        -\/(SendingCredentialsFailed)
    }
  }
}
