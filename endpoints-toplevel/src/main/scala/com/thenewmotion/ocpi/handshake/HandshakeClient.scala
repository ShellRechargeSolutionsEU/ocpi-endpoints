package com.thenewmotion.ocpi.handshake

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import com.thenewmotion.ocpi.common.OcpiClient
import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{SuccessWithDataResp, Url}
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.ActorMaterializer

class HandshakeClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getTheirVersions(uri: Uri, token: String)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ List[Version]] =
    singleRequest[SuccessWithDataResp[List[Version]]](Get(uri), token).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the versions information from $uri with token $token. Reason: $err")
        VersionsRetrievalFailed
      }, _.data)
    }

  def getTheirVersionDetails(uri: Uri, token: String)
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ VersionDetails] =
    singleRequest[SuccessWithDataResp[VersionDetails]](Get(uri), token).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the version details from $uri with token $token. Reason: $err")
        VersionDetailsRetrievalFailed
      }, _.data)
    }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: String, credToConnectToUs: Creds)
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ Creds] = {
    singleRequest[SuccessWithDataResp[Creds]](Post(theirCredUrl, credToConnectToUs), tokenToConnectToThem).map {
      _.bimap(err => {
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: $err")
        SendingCredentialsFailed
      }, _.data)
    }
  }
}
