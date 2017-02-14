package com.thenewmotion.ocpi

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.thenewmotion.ocpi.TopLevelRouteRejections._
import com.thenewmotion.ocpi.common.{OcpiExceptionHandler, TokenAuthenticator}
import com.thenewmotion.ocpi.handshake.InitiateHandshakeRoute
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.{GlobalPartyId, SuccessWithDataResp}
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

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
        v.keys.map(x => Version(x, appendPath(uri, x.name).toString())).toList)
      )
    case _ => reject(NoVersionsRejection())
  }

  def versionDetailsRoute(version: VersionNumber, versionInfo: OcpiVersionConfig, uri: Uri, apiUser: GlobalPartyId): Route =
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


  def topLevelRoute(implicit ec: ExecutionContext, mat: ActorMaterializer) = {
    val externalUseToken = new TokenAuthenticator(routingConfig.authenticateApiUser)
    val internalUseToken = new TokenAuthenticator(routingConfig.authenticateInternalUser)

    // TODO top level route contains code that is mostly important
    // for the registration part of OCPI. This should be moved to a separate module.
    (handleRejections(TopLevelRouteRejections.Handler) & handleExceptions(OcpiExceptionHandler.Default)) {
      (pathPrefix(routingConfig.namespace) & extract(_.request.uri)) { uri =>
        pathPrefix("initiateHandshake") {
          pathEndOrSingleSlash {
            authenticateOrRejectWithChallenge(internalUseToken) { _: GlobalPartyId =>
              new InitiateHandshakeRoute(routingConfig.handshakeService).route
            }
          }
        } ~
        authenticateOrRejectWithChallenge(externalUseToken) { apiUser: GlobalPartyId =>
          pathPrefix(EndpointIdentifier.Versions.name) {
            pathEndOrSingleSlash {
              versionsRoute(uri)
            } ~
            pathPrefix(Segment) { version =>
              val route = for {
                existingVersion <- VersionNumber.withName(version)
                supportedVersion <- routingConfig.versions.get(existingVersion)
              } yield versionDetailsRoute(existingVersion, supportedVersion, uri, apiUser)
              route getOrElse reject(UnsupportedVersionRejection(version))
            }
          }
        }
      }
    }
  }
}
