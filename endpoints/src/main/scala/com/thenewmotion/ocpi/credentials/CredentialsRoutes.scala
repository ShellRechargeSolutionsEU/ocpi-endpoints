package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.CurrentTimeComponent
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.SuccessResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import spray.routing.HttpService
import scalaz._

trait CredentialsRoutes extends HttpService with LazyLogging with CurrentTimeComponent {

  val cdh: CredentialsDataHandler


  def credentialsRoute(version: String, auth: String) = {
    import spray.httpx.SprayJsonSupport._
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
    path("credentials") {
        post {
          entity(as[Creds]) { clientCreds =>
            cdh.registerVersionsEndpoint(version, auth, Credentials.fromOcpiClass(clientCreds)) match {
              case -\/(CouldNotRegisterParty) => reject()
              case \/-(newCreds) => complete(CredsResp(GenericSuccess.code,Some(GenericSuccess.default_message),
                currentTime.instance, newCreds))
            }
          }
      }
    }
  }
}
