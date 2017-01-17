package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.SuccessWithDataResp

class HandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Credentials._

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds]) { credsToConnectToThem =>
        complete {
          service
            .reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)
            .mapRight(SuccessWithDataResp(GenericSuccess, None, currentTime, _))
        }
      }
    } ~
    get {
      complete {
        service
          .credsToConnectToUs(tokenToConnectToUs)
          .map(SuccessWithDataResp(GenericSuccess, None, currentTime, _))
      }
    } ~
    put {
      entity(as[Creds]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)
            .mapRight(SuccessWithDataResp(GenericSuccess, None, currentTime, _))
        }
      }
    }
  }
}

class InitiateHandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_1.Versions._

  def route(implicit ec: ExecutionContext) = {
    post {
      entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
        complete {
          import theirVersionsUrlInfo._
          service
            .initiateHandshakeProcess(partyName, countryCode, partyId, token, url)
            .mapRight(SuccessWithDataResp(GenericSuccess, None, currentTime, _))
        }
      }
    }
  }
}
