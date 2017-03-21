package com.thenewmotion.ocpi
package tokens

import akka.http.scaladsl._
import akka.http.scaladsl.model.Uri
import akka.stream.ActorMaterializer
import client.RequestBuilding._
import com.thenewmotion.ocpi.msgs.{ErrorResp, SuccessWithDataResp}
import msgs.v2_1.Tokens.{AuthorizationInfo, LocationReferences}
import com.thenewmotion.ocpi.common.{ClientObjectUri, OcpiClient}

import scala.concurrent._
import scalaz.\/

class MspTokensClient(implicit http: HttpExt) extends OcpiClient {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def authorize(endpointUri: Uri, authToken: String, tokenUid: String, locationReferences: Option[LocationReferences])
               (implicit ec: ExecutionContext, mat: ActorMaterializer): Future[ErrorResp \/ AuthorizationInfo] = {
    val authorizeUri = endpointUri.withPath(endpointUri.path / tokenUid / "authorize")
    singleRequest[SuccessWithDataResp[AuthorizationInfo]](Post(authorizeUri, locationReferences), authToken) map {
      _.bimap({ err =>
        logger.error(s"Error getting real-time authorization from $authorizeUri: $err")
        err
      }, _.data)
    }
  }
}
