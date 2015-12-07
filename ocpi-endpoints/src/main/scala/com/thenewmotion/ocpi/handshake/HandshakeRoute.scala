package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, ourVersionsUrl: String, currentTime: => DateTime = DateTime.now) extends JsonApi {

  def route(accessedVersion: Version, tokenToConnectToUs: AuthToken)(implicit ec: ExecutionContext) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
    import com.thenewmotion.ocpi.msgs.v2_0.Versions._

    (post & extract(_.request.uri)) { ourCredentialsUrl =>
      entity(as[Creds]) { credsToConnectToThem =>
        onSuccess(service.reactToHandshakeRequest(accessedVersion, tokenToConnectToUs, credsToConnectToThem, ourVersionsUrl)) {
          case -\/(_) => reject()
          case \/-(ourNewCredsForThem) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
            currentTime, ourNewCredsForThem))
        }
      }
    } ~
      path("initiateHandshake"){
        post {
          entity(as[VersionsRequest]) { theirVersionDetails =>
            onSuccess(service.initiateHandshakeProcess(theirVersionDetails.token, theirVersionDetails.url)) {
              case -\/(_) => reject()
              case \/-(credsToConnectToUs) => complete(credsToConnectToUs)
            }
          }
        }
      }
  }
}
