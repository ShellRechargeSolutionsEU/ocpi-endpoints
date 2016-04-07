package com.thenewmotion.ocpi.handshake

import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.Url
import com.thenewmotion.ocpi.msgs.v2_0.Credentials.{Creds, CredsResp}
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}

class HandshakeClient(implicit refFactory: ActorRefFactory, timeout: Timeout = Timeout(20.seconds)) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  def getTheirVersions(uri: Uri, token: String)(implicit ec: ExecutionContext): Future[HandshakeError \/ VersionsResp] = {
    val pipeline = request(token) ~> unmarshal[VersionsResp]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case Success(versions) => \/-(versions)
      case Failure(t) =>
        logger.error(s"Could not retrieve the versions information from $uri with token $token", t)
        -\/(VersionsRetrievalFailed)
    }
  }

  def getTheirVersionDetails(uri: Uri, token: String)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetailsResp] = {
    val pipeline = request(token) ~> unmarshal[VersionDetailsResp]
    val resp = pipeline(Get(uri))
    bimap(resp) {
      case Success(versionDet) => \/-(versionDet)
      case Failure(t) =>
        logger.error(s"Could not retrieve the version details from $uri with token $token", t)
        -\/(VersionDetailsRetrievalFailed)
    }
  }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: String, credToConnectToUs: Creds)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ CredsResp] = {
    val pipeline = request(tokenToConnectToThem) ~> unmarshal[CredsResp]
    val resp = pipeline(Post(theirCredUrl, credToConnectToUs))
    bimap(resp) {
      case Success(theirCreds) => \/-(theirCreds)
      case Failure(t) =>
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs", t )
        -\/(SendingCredentialsFailed)
    }
  }
}
