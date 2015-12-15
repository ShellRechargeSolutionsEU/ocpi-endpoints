package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
    import com.thenewmotion.ocpi.msgs.v2_0.Versions._

    (post & extract(_.request.uri)) { ourCredentialsUrl =>
      entity(as[Creds]) { credsToConnectToThem =>
        onSuccess(service.reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem)) {
          case -\/(_) => reject()
          case \/-(ourNewCredsForThem) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
            currentTime, ourNewCredsForThem))
        }
      }
    } ~
      path("initiateHandshake"){
        post {
          entity(as[VersionsRequest]) { theirVersionsUrlInfo =>
            onSuccess(service.initiateHandshakeProcess(theirVersionsUrlInfo.token, theirVersionsUrlInfo.url)) {
              case -\/(_) => reject()
              case \/-(credsToConnectToUs) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
                currentTime, credsToConnectToUs))
            }
          }
        }
      }
  }
}
