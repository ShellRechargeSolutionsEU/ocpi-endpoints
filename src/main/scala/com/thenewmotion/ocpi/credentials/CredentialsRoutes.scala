package com.thenewmotion.ocpi.credentials

import com.thenewmotion.ocpi.{CouldNotRegisterParty, CurrentTimeComponent}
import com.typesafe.scalalogging.LazyLogging
import spray.http.StatusCodes
import spray.routing.HttpService
import scalaz._

trait CredentialsRoutes extends HttpService with LazyLogging with CurrentTimeComponent {

  val cdh: CredentialsDataHandler


  def credentialsRoute = {
    import spray.httpx.SprayJsonSupport._
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Credentials._
    path("credentials") {
      post {
        entity(as[Creds]) {creds =>
          cdh.registerParty(Credentials.fromOcpiClass(creds)) match {
            case -\/(CouldNotRegisterParty) => reject()
            case _ => complete(StatusCodes.OK)
          }
        }
      }
    }
  }
}
