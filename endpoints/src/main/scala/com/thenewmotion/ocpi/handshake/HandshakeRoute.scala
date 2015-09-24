package com.thenewmotion.ocpi
package handshake

import com.thenewmotion.ocpi.JsonApi
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext
import scalaz._

class HandshakeRoute(service: HandshakeService, currentTime: => DateTime = DateTime.now) extends JsonApi {
  
  def route(version: Version, auth: AuthToken)(implicit ec: ExecutionContext) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._

    (post & extract(_.request.uri)) { uri =>
      entity(as[Creds]) { clientCreds =>
        onSuccess(service.startHandshake(version, auth, clientCreds, uri)) {
          case -\/(_) => reject()
          case \/-(newCreds) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
            currentTime, newCreds))
        }
      }
    }
  }
}
