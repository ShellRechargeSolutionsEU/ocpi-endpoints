package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.model.Uri
import common.OcpiClient
import registration.RegistrationError._
import msgs.{AuthToken, SuccessWithDataResp, Url}
import msgs.v2_1.Credentials.Creds
import msgs.Versions._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.\/
import akka.http.scaladsl.client.RequestBuilding._
import akka.stream.ActorMaterializer
import msgs.Ownership.{Ours, Theirs}

class RegistrationClient(implicit http: HttpExt) extends OcpiClient {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  def getTheirVersions(uri: Uri, token: AuthToken[Ours])
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ List[Version]] =
    singleRequest[SuccessWithDataResp[List[Version]]](Get(uri), token.value).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the versions information from $uri with token $token. Reason: $err")
        VersionsRetrievalFailed
      }, _.data)
    }

  def getTheirVersionDetails(uri: Uri, token: AuthToken[Ours])
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ VersionDetails] =
    singleRequest[SuccessWithDataResp[VersionDetails]](Get(uri), token.value).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve the version details from $uri with token $token. Reason: $err")
        VersionDetailsRetrievalFailed
      }, _.data)
    }

  def sendCredentials(theirCredUrl: Url, tokenToConnectToThem: AuthToken[Ours], credToConnectToUs: Creds[Ours])
      (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {
    singleRequest[SuccessWithDataResp[Creds[Theirs]]](Post(theirCredUrl, credToConnectToUs), tokenToConnectToThem.value).map {
      _.bimap(err => {
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: $err")
        SendingCredentialsFailed
      }, _.data)
    }
  }

  def updateCredentials(theirCredUrl: Url, tokenToConnectToThem: AuthToken[Ours], credToConnectToUs: Creds[Ours])
    (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {
    singleRequest[SuccessWithDataResp[Creds[Theirs]]](Put(theirCredUrl, credToConnectToUs), tokenToConnectToThem.value).map {
      _.bimap(err => {
        logger.error( s"Could not retrieve their credentials from $theirCredUrl with token" +
          s"$tokenToConnectToThem when sending our credentials $credToConnectToUs. Reason: $err")
        UpdatingCredentialsFailed
      }, _.data)
    }
  }
}
