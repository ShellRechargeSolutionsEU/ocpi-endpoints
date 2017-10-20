package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.stream.Materializer
import client.RequestBuilding._
import com.thenewmotion.ocpi.msgs.{AuthToken, SuccessResp}
import msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences, TokenUid}
import com.thenewmotion.ocpi.common.{ErrUnMar, OcpiClient}
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import cats.syntax.either._

import scala.concurrent._

class MspTokensClient(
  implicit http: HttpExt,
  successU: FromEntityUnmarshaller[SuccessResp[AuthorizationInfo]],
  errorU: ErrUnMar,
  locRefM: ToEntityMarshaller[LocationReferences]
) extends OcpiClient {

  def authorize(
    endpointUri: Uri,
    authToken: AuthToken[Ours],
    tokenUid: TokenUid,
    locationReferences: Option[LocationReferences]
  )(
    implicit ec: ExecutionContext,
    mat: Materializer
  ): Future[ErrorRespOr[AuthorizationInfo]] = {
    val authorizeUri = endpointUri.withPath(endpointUri.path / tokenUid.value / "authorize")
    singleRequest[AuthorizationInfo](Post(authorizeUri, locationReferences), authToken) map {
      _.bimap({ err =>
        logger.error(s"Error getting real-time authorization from $authorizeUri: $err")
        err
      }, _.data)
    }
  }
}
