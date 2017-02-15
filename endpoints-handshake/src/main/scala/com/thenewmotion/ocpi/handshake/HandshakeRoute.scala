package com.thenewmotion.ocpi
package handshake

import msgs.OcpiStatusCode.GenericSuccess
import scala.concurrent.ExecutionContext
import ErrorMarshalling._
import akka.stream.ActorMaterializer
import msgs.{GlobalPartyId, OurAuthToken, SuccessWithDataResp}

class HandshakeRoute(service: HandshakeService)(implicit mat: ActorMaterializer) extends JsonApi {
  import msgs.v2_1.OcpiJsonProtocol._
  import msgs.v2_1.Credentials._

  def route(accessedVersion: Version, user: GlobalPartyId)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds[OurAuthToken]]) { credsToConnectToThem =>
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
      entity(as[Creds[OurAuthToken]]) { credsToConnectToThem =>
        complete {
          service
            .reactToUpdateCredsRequest(accessedVersion, user, credsToConnectToThem)
            .mapRight(x => SuccessWithDataResp(GenericSuccess, data = x))
        }
      }
    }
  }
}
