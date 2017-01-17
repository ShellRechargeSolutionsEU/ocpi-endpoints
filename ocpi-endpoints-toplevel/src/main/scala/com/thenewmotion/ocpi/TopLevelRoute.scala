package com.thenewmotion.ocpi

import handshake.InitiateHandshakeRoute
import msgs.v2_1.OcpiStatusCode
import OcpiStatusCode._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.headers.{GenericHttpCredentials, HttpChallenge, HttpCredentials}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.SecurityDirectives._
import msgs.v2_1.Versions._
import msgs.v2_1.CommonTypes.SuccessWithDataResp
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime

trait TopLevelRoute extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def routingConfig: OcpiRoutingConfig

  def currentTime = DateTime.now

  val EndPointPathMatcher = Segment.flatMap(s => EndpointIdentifier.withName(s))

  def appendPath(uri: Uri, segments: String*) = {
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) if path.toString.endsWith("/") => path + add
      case (path, add) => path / add
    })
  }

  def versionsRoute(uri: Uri): Route = routingConfig.versions match {
    case v if v.nonEmpty =>
      complete(SuccessWithDataResp(
        GenericSuccess,
        None,
        currentTime,
        v.keys.flatMap(x => VersionNumber.withName(x).map(Version(_, appendPath(uri, x).toString()))).toList)
      )
    case _ => reject(NoVersionsRejection())
  }

  def versionDetailsRoute(version: VersionNumber, versionInfo: OcpiVersionConfig, uri: Uri, apiUser: ApiUser): Route =
    pathEndOrSingleSlash {
      complete(
        SuccessWithDataResp(
          GenericSuccess,
          None,
          currentTime,
          VersionDetails(
            version, versionInfo.endPoints.map {
              case (k, Right(v)) => Endpoint(k, appendPath(uri, k.name).toString() )
              case (k, Left(extUri)) => Endpoint(k, extUri)
            }
          )
        )
      )
    } ~
    pathPrefix(EndPointPathMatcher) { path =>
      versionInfo.endPoints.get(path) match {
        case None => reject
        case Some(Left(_)) => reject // implemented externally
        case Some(Right(route)) => route(version, apiUser)
      }
    }


  def topLevelRoute(implicit ec: ExecutionContext) = {
    val externalUseToken = new TokenAuthenticator(routingConfig.authenticateApiUser)
    val internalUseToken = new TokenAuthenticator(routingConfig.authenticateInternalUser)

    (handleRejections(OcpiRejectionHandler.Default) & handleExceptions(OcpiExceptionHandler.Default)) {
      (pathPrefix(routingConfig.namespace) & extract(_.request.uri)) { uri =>
        pathPrefix("initiateHandshake") {
          pathEndOrSingleSlash {
            authenticateOrRejectWithChallenge(internalUseToken) { _: ApiUser =>
              new InitiateHandshakeRoute(routingConfig.handshakeService).route
            }
          }
        } ~
        authenticateOrRejectWithChallenge(externalUseToken) { apiUser: ApiUser =>
          pathPrefix(EndpointIdentifier.Versions.name) {
            pathEndOrSingleSlash {
              versionsRoute(uri)
            } ~
            pathPrefix(Segment) { version =>
              val route = for {
                existingVersion <- VersionNumber.withName(version)
                supportedVersion <- routingConfig.versions.get(existingVersion.name)
              } yield versionDetailsRoute(existingVersion, supportedVersion, uri, apiUser)
              route getOrElse reject(UnsupportedVersionRejection(version))
            }
          }
        }
      }
    }
  }
}

class TokenAuthenticator(
  apiUser: String => Option[ApiUser]
) extends (Option[HttpCredentials] ⇒ Future[AuthenticationResult[ApiUser]]) {
  override def apply(credentials: Option[HttpCredentials]): Future[AuthenticationResult[ApiUser]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future(
      credentials
        .flatMap {
          case GenericHttpCredentials("Token", token, _) => Some(token)
          case _ => None
        } flatMap apiUser match {
        case Some(x) => Right(x)
        case None => Left(HttpChallenge(scheme = "Token", realm = "ocpi"))
      }
    )
  }
}