package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.BusinessDetails
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    post {
      entity(as[Creds]) { credsToConnectToThem =>
        onSuccess(service.reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)) {
          case -\/(_) => reject()
          case \/-(newCredsToConnectToUs) => complete(CredsResp(GenericSuccess.code, Some(GenericSuccess.default_message),
            currentTime, newCredsToConnectToUs))
        }
      }
    } ~
      get {
        service.findRegisteredCredsToConnectToUs(tokenToConnectToUs) match {
          case -\/(_) => reject()
          case \/-(credsToConnectToUs) =>
            complete(CredsResp(GenericSuccess.code, None, currentTime, credsToConnectToUs))
        }
      } ~
      put {
        entity(as[Creds]) { credsToConnectToThem =>
          onSuccess(service.reactToUpdateCredsRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)) {
            case -\/(_) => reject()
            case \/-(newCredsToConnectToUs) => complete(CredsResp(GenericSuccess.code, Some(GenericSuccess.default_message),
              currentTime, newCredsToConnectToUs))
          }
        }
      }
  }
}

class InitiateHandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
  import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
  import com.thenewmotion.ocpi.msgs.v2_0.Versions._

  def route(implicit ec: ExecutionContext) = {
    post {
      entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
        onSuccess(service.initiateHandshakeProcess(theirVersionsUrlInfo.token, theirVersionsUrlInfo.url)) {
          case -\/(_) => reject()
          case \/-(newCredToConnectToThem) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
            currentTime, newCredToConnectToThem))
        }
      }
    }
  }
}