package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericSuccess
import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp

class HandshakeRoute(service: HandshakeService) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Credentials._

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds]) { credsToConnectToThem =>
        complete {
          service
            .reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    } ~
    get {
      complete {
        service
          .credsToConnectToUs(tokenToConnectToUs)
          .map(x => SuccessWithDataResp(GenericSuccess, data = x))
      }
    } ~
    put {
      entity(as[Creds]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}

class InitiateHandshakeRoute(service: HandshakeService) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Versions._

  def route(implicit ec: ExecutionContext) = {
    post {
      entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
        complete {
          import theirVersionsUrlInfo._
          service
            .initiateHandshakeProcess(partyName, countryCode, partyId, token, url)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}
