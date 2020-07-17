package com.thenewmotion.ocpi
package tokens

import _root_.akka.http.scaladsl._
import _root_.akka.http.scaladsl.marshalling.ToEntityMarshaller
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import client.RequestBuilding._
import com.thenewmotion.ocpi.msgs.AuthToken
import msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, TokenUid}
import com.thenewmotion.ocpi.common.{ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import cats.syntax.either._
import scala.concurrent._

class MspTokensClient(
  implicit http: HttpExt,
  successU: SuccessRespUnMar[AuthorizationInfo],
  errorU: ErrRespUnMar,
  locRefM: ToEntityMarshaller[LocationReferences]
) extends OcpiClient {

  def authorize(
    endpointUri: Uri,
    authToken: AuthToken[Ours],
    tokenUid: TokenUid,
    locationReferences: Option[LocationReferences]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[AuthorizationInfo]] = {
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
