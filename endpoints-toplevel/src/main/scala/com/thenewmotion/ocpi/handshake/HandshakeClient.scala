package com.thenewmotion.ocpi
package handshake

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.OcpiClient
import handshake.HandshakeError._
import msgs.v2_1.CommonTypes.{SuccessWithDataResp, Url}
import msgs.v2_1.Credentials.{Creds, OurToken, TheirToken}
import msgs.v2_1.Versions._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.ActorMaterializer

class HandshakeClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getTheirVersions(uri: Uri, token: OurToken)
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ List[Version]] =
    singleRequest[SuccessWithDataResp[List[Version]]](Get(uri), token.value).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the versions information from $uri with token $token. Reason: $err")
        VersionsRetrievalFailed
      }, _.data)
    }

  def getTheirVersionDetails(uri: Uri, token: OurToken)
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ VersionDetails] =
    singleRequest[SuccessWithDataResp[VersionDetails]](Get(uri), token.value).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the version details from $uri with token $token. Reason: $err")
        VersionDetailsRetrievalFailed
      }, _.data)
    }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: OurToken, credToConnectToUs: Creds[TheirToken])
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[HandshakeError \/ Creds[OurToken]] = {
    singleRequest[SuccessWithDataResp[Creds[OurToken]]](Post(theirCredUrl, credToConnectToUs), tokenToConnectToThem.value).map {
      _.bimap(err => {
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: $err")
        SendingCredentialsFailed
      }, _.data)
    }
  }
}
