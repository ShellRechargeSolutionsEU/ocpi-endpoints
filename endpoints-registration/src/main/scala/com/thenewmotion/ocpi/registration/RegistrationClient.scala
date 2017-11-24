package com.thenewmotion.ocpi
package registration

import scala.concurrent.{ExecutionContext, Future}
import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import common.{ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import msgs.Ownership.{Ours, Theirs}
import msgs.Versions._
import msgs.v2_1.Credentials.Creds
import msgs.{AuthToken, Url}
import registration.RegistrationError._
import cats.syntax.either._

class RegistrationClient(
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  sucListVerU: SuccessRespUnMar[List[Version]],
  sucVerDetU: SuccessRespUnMar[VersionDetails],
  sucCredsU: SuccessRespUnMar[Creds[Theirs]],
  credsM: ToEntityMarshaller[Creds[Ours]]
) extends OcpiClient {

  def getTheirVersions(
    uri: Uri,
    token: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[Either[RegistrationError, List[Version]]] = {
    def errorMsg = s"Could not retrieve the versions information from $uri with token $token."
    val regError = VersionsRetrievalFailed

    singleRequest[List[Version]](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.asLeft }
  }

  def getTheirVersionDetails(
    uri: Uri,
    token: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[Either[RegistrationError, VersionDetails]] = {
    def errorMsg = s"Could not retrieve the version details from $uri with token $token."
    val regError = VersionDetailsRetrievalFailed

    singleRequest[VersionDetails](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.asLeft }
  }

  def sendCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[Either[RegistrationError, Creds[Theirs]]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token " +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError = SendingCredentialsFailed

    singleRequest[Creds[Theirs]](
      Post(theirCredUrl.value, credToConnectToUs), tokenToConnectToThem
    ) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.asLeft }
  }

  def updateCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[Either[RegistrationError, Creds[Theirs]]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token" +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError = UpdatingCredentialsFailed

    singleRequest[Creds[Theirs]](
      Put(theirCredUrl.value, credToConnectToUs), tokenToConnectToThem
    ) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } recover { case t => logger.error(errorMsg, t); regError.asLeft }
  }
}
