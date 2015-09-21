package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.handshake.{HandshakeClient, HandshakeDataHandler, HandshakeService, HandshakeRoutes}
import com.thenewmotion.ocpi.locations.LocationsRoutes
import com.thenewmotion.ocpi.versions.VersionsRoutes
import spray.routing._
import spray.routing.authentication.Authentication

import scala.concurrent.{ExecutionContext, Future}

abstract class OcpiRestActor extends HttpServiceActor with TopLevelRoutes {

  implicit private val rejectionHandler: RejectionHandler = OcpiRejectionHandler.Default

  override def receive: Receive =
    runRoute(allRoutes )
}

trait TopLevelRoutes extends HttpService
  with VersionsRoutes with HandshakeRoutes
  with LocationsRoutes with CurrentTimeComponent {
  import scala.concurrent.ExecutionContext.Implicits.global

  val tldh: TopLevelRouteDataHandler
  val adh: AuthDataHandler
  val client: HandshakeClient
  val hdh: HandshakeDataHandler
  val handshakeService = new HandshakeService(client, hdh)

  lazy val auth = new Authenticator(adh)
  val currentTime = new CurrentTime

  def allRoutes =
    headerValueByName("Authorization") { access_token =>
      authenticate(auth.validate(access_token)) { apiUser: ApiUser =>
        pathPrefix(tldh.namespace) {
          versionsRoute ~
          pathPrefix(Segment) { version =>
            pathEndOrSingleSlash{
              versionDetailsRoute(version)
            } ~
            handshakeRoute(version, apiUser.token) ~ locationsRoute(version)
          }
        }
      }
    }
}

class Authenticator(adh: AuthDataHandler)(implicit ec: ExecutionContext) {

  def validate(token: String): Future[Authentication[ApiUser]] = {
    Future {
      extractTokenValue(token) match {
        case Some(tokenVal) => adh.authenticateApiUser(tokenVal)
          .toRight(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        case None => Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
      }
    }
  }

  def extractTokenValue(token: String): Option[String] = {
    val authScheme = "Token "
    if (token.startsWith(authScheme)){
      val tokenVal = token.substring(authScheme.length)
      if (!tokenVal.isEmpty) Some(tokenVal) else None
    }
    else None
  }
}

trait CurrentTimeComponent {
  import org.joda.time.DateTime
  val currentTime: CurrentTime
  class CurrentTime {
    def instance = DateTime.now()
  }
}