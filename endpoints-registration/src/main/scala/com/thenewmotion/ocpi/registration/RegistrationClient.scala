package com.thenewmotion.ocpi
package registration

import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.Ownership.{Ours, Theirs}
import com.thenewmotion.ocpi.msgs.Versions._
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.Creds
import com.thenewmotion.ocpi.msgs.{AuthToken, Url}
import com.thenewmotion.ocpi.registration.RegistrationError._
import scala.concurrent.ExecutionContext

class RegistrationClient[F[_]: Async](
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  sucListVerU: SuccessRespUnMar[List[Version]],
  sucVerDetU: SuccessRespUnMar[VersionDetails],
  sucCredsU: SuccessRespUnMar[Creds[Theirs]],
  credsM: ToEntityMarshaller[Creds[Ours]]
) extends OcpiClient[F] {

  def getTheirVersions(
    uri: Uri,
    token: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[Either[RegistrationError, List[Version]]] = {
    def errorMsg = s"Could not retrieve the versions information from $uri with token $token."
    val regError: RegistrationError = VersionsRetrievalFailed

    singleRequest[List[Version]](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } handleErrorWith { t => logger.error(errorMsg, t); Async[F].pure(regError.asLeft) }
  }

  def getTheirVersionDetails(
    uri: Uri,
    token: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[Either[RegistrationError, VersionDetails]] = {
    def errorMsg = s"Could not retrieve the version details from $uri with token $token."
    val regError: RegistrationError = VersionDetailsRetrievalFailed

    singleRequest[VersionDetails](Get(uri), token) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } handleErrorWith { t => logger.error(errorMsg, t); Async[F].pure(regError.asLeft) }
  }

  def sendCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[Either[RegistrationError, Creds[Theirs]]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token " +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError: RegistrationError = SendingCredentialsFailed

    singleRequest[Creds[Theirs]](
      Post(theirCredUrl.value, credToConnectToUs), tokenToConnectToThem
    ) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } handleErrorWith { t => logger.error(errorMsg, t); Async[F].pure(regError.asLeft) }
  }

  def updateCredentials(
    theirCredUrl: Url,
    tokenToConnectToThem: AuthToken[Ours],
    credToConnectToUs: Creds[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[Either[RegistrationError, Creds[Theirs]]] = {
    def errorMsg = s"Could not retrieve their credentials from $theirCredUrl with token" +
      s"$tokenToConnectToThem when sending our credentials $credToConnectToUs."
    val regError: RegistrationError = UpdatingCredentialsFailed

    singleRequest[Creds[Theirs]](
      Put(theirCredUrl.value, credToConnectToUs), tokenToConnectToThem
    ) map {
      _.bimap(err => {
        logger.error(errorMsg + s" Reason: $err"); regError
      }, _.data)
    } handleErrorWith { t => logger.error(errorMsg, t); Async[F].pure(regError.asLeft) }
  }
}
