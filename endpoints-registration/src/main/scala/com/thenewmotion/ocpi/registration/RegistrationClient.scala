package com.thenewmotion.ocpi
package registration

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import common.OcpiClient
import msgs.Ownership.{Ours, Theirs}
import msgs.Versions._
import msgs.v2_1.Credentials.Creds
import msgs.{AuthToken, SuccessWithDataResp, Url}
import registration.RegistrationError._
import scalaz._
import Scalaz._

class RegistrationClient(implicit http: HttpExt) extends OcpiClient {
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def getTheirVersions(
    uri: Uri,
    token: AuthToken[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ List[Version]] = {
    def errorMsg = s"Could not retrieve the versions information from $uri with token $token."
    val regError = VersionsRetrievalFailed

    singleRequest[SuccessWithDataResp[List[Version]]](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.left }
  }

  def getTheirVersionDetails(
    uri: Uri,
    token: AuthToken[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ VersionDetails] = {
    def errorMsg = s"Could not retrieve the version details from $uri with token $token."
    val regError = VersionDetailsRetrievalFailed

    singleRequest[SuccessWithDataResp[VersionDetails]](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.left }
  }

  def sendCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token " +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError = SendingCredentialsFailed

    singleRequest[SuccessWithDataResp[Creds[Theirs]]](Post(theirCredUrl, credToConnectToUs), tokenToConnectToThem) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.left }
  }

  def updateCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(implicit ec: ExecutionContext, mat: ActorMaterializer): Future[RegistrationError \/ Creds[Theirs]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token" +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError = UpdatingCredentialsFailed

    singleRequest[SuccessWithDataResp[Creds[Theirs]]](Put(theirCredUrl, credToConnectToUs), tokenToConnectToThem) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.left }
  }
}
