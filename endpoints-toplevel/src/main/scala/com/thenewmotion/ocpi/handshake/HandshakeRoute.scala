package com.thenewmotion.ocpi
package handshake

import msgs.v2_1.OcpiStatusCode.GenericSuccess
import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import akka.stream.ActorMaterializer
import msgs.v2_1.CommonTypes.{GlobalPartyId, SuccessWithDataResp}

class HandshakeRoute(service: HandshakeService)(implicit mat: ActorMaterializer) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Credentials._

  def route(accessedVersion: Version, user: GlobalPartyId)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds[OurToken]]) { credsToConnectToThem =>
        complete {
          service
            .reactToHandshakeRequest(accessedVersion, user, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    } ~
    get {
      complete {
        service
          .credsToConnectToUs(user)
          .map(x => SuccessWithDataResp(GenericSuccess, data = x))
      }
    } ~
    put {
      entity(as[Creds[OurToken]]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(accessedVersion, user, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}

class InitiateHandshakeRoute(service: HandshakeService)(implicit mat: ActorMaterializer) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Versions._

  def route(implicit ec: ExecutionContext) = {
    post {
      entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
        complete {
          import theirVersionsUrlInfo._
          service
            .initiateHandshakeProcess(partyName, GlobalPartyId(countryCode, partyId), token, url)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}
