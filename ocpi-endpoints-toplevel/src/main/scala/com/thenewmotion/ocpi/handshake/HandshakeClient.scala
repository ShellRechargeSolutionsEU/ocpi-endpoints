package com.thenewmotion.ocpi.handshake

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{SuccessWithDataResp, Url}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalaz.{-\/, \/, \/-}
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.ActorMaterializer

class HandshakeClient(implicit actorSystem: ActorSystem, materializer: ActorMaterializer) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getTheirVersions(uri: Uri, token: String)(implicit ec: ExecutionContext): Future[HandshakeError \/ List[Version]] = {
    val resp = singleRequest[SuccessWithDataResp[List[Version]]](Get(uri), token)
    bimap(resp) {
      case Success(versions) => \/-(versions.data)
      case Failure(t) =>
        logger.error(s"Could not retrieve the versions information from $uri with token $token. Reason: ${t.getLocalizedMessage}", t)
        -\/(VersionsRetrievalFailed)
    }
  }

  def getTheirVersionDetails(uri: Uri, token: String)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ VersionDetails] = {
    val resp = singleRequest[SuccessWithDataResp[VersionDetails]](Get(uri), token)
    bimap(resp) {
      case Success(versionDet) => \/-(versionDet.data)
      case Failure(t) =>
        logger.error(s"Could not retrieve the version details from $uri with token $token. Reason: ${t.getLocalizedMessage}", t)
        -\/(VersionDetailsRetrievalFailed)
    }
  }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: String, credToConnectToUs: Creds)
      (implicit ec: ExecutionContext): Future[HandshakeError \/ Creds] = {
    val resp = singleRequest[SuccessWithDataResp[Creds]](Post(theirCredUrl, credToConnectToUs), tokenToConnectToThem)
    bimap(resp) {
      case Success(theirCreds) => \/-(theirCreds.data)
      case Failure(t) =>
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: ${t.getLocalizedMessage}", t )
        -\/(SendingCredentialsFailed)
    }
  }
}
