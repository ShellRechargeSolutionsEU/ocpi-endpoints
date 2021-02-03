package com.thenewmotion.ocpi
package tokens

import _root_.akka.http.scaladsl._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import akka.http.scaladsl.client.RequestBuilding._
import cats.effect.{Async, ContextShift}
import cats.syntax.either._
import cats.syntax.functor._
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, TokenUid}
import scala.concurrent._

class MspTokensClient[F[_]: Async](
  implicit http: HttpExt,
  successU: SuccessRespUnMar[AuthorizationInfo],
  errorU: ErrRespUnMar,
  locRefM: ToEntityMarshaller[LocationReferences]
) extends OcpiClient[F] {

  def authorize(
    endpointUri: Uri,
    authToken: AuthToken[Ours],
    tokenUid: TokenUid,
    locationReferences: Option[LocationReferences]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[F],
    mat: Materializer
  ): F[ErrorRespOr[AuthorizationInfo]] = {
    val oldPath = endpointUri.path
    val authorizeUri = endpointUri.withPath {
      (if (oldPath.endsWithSlash) oldPath + tokenUid.value else  oldPath / tokenUid.value) / "authorize"
    }
    singleRequest[AuthorizationInfo](Post(authorizeUri, locationReferences), authToken) map {
      _.bimap({ err =>
        logger.error(s"Error getting real-time authorization from $authorizeUri: $err")
        err
      }, _.data)
    }
  }
}
