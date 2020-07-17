package com.thenewmotion.ocpi
package tokens

import _root_.akka.http.scaladsl.HttpExt
import _root_.akka.http.scaladsl.client.RequestBuilding._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.{HttpRequest, Uri}
import _root_.akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import com.thenewmotion.ocpi.common.{ClientObjectUri, ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{Token, TokenPatch, TokenUid}
import scala.concurrent.ExecutionContext
import cats.syntax.either._

/**
  * Client that can be used by the MSP side
  * for pushing tokens to the CPO
  */
class CpoTokensClient(
  implicit http: HttpExt,
  errorU: ErrRespUnMar,
  successTokenU: SuccessRespUnMar[Token],
  successUnitU: SuccessRespUnMar[Unit],
  tokenM: ToEntityMarshaller[Token],
  tokenPM: ToEntityMarshaller[TokenPatch]
) extends OcpiClient {

  def getToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    tokenUid: TokenUid
  )(implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Token]] =
    singleRequest[Token](Get(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve token from ${tokenUri.value}. Reason: $err")
        err
      }, _.data)
    }

  private def push(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    rb: Uri => HttpRequest
  )(implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    singleRequest[Unit](rb(tokenUri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not upload token to ${tokenUri.value}. Reason: $err")
        err
      }, _ => ())
    }

  def uploadToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    token: Token
  )(implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    push(tokenUri, authToken, uri => Put(uri, token))

  def updateToken(
    tokenUri: ClientObjectUri,
    authToken: AuthToken[Ours],
    patch: TokenPatch
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    push(tokenUri, authToken, uri => Patch(uri, patch))

}

