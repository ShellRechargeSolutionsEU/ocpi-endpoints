package com.thenewmotion.ocpi

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import VersionRejections._
import VersionsRoute.OcpiVersionConfig
import common.OcpiExceptionHandler
import msgs.{GlobalPartyId, SuccessWithDataResp}
import msgs.OcpiStatusCode._
import msgs.Versions._
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext

object VersionsRoute {
  case class OcpiVersionConfig(
    endPoints: Map[EndpointIdentifier, Either[URI, GuardedRoute]]
  )
}

class VersionsRoute(versions: => Map[VersionNumber, OcpiVersionConfig]) extends JsonApi {
  import VersionsRoute._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def currentTime = DateTime.now

  val EndPointPathMatcher = Segment.map(EndpointIdentifier(_))

  def appendPath(uri: Uri, segments: String*) = {
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) if path.toString.endsWith("/") => path + add
      case (path, add) => path / add
    })
  }

  def versionsRoute(uri: Uri): Route = versions match {
    case v if v.nonEmpty =>
      complete(SuccessWithDataResp(
        GenericSuccess,
        None,
        currentTime,
        v.keys.map(x => Version(x, appendPath(uri, x.toString).toString())).toList)
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
              case (k, Right(v)) => Endpoint(k, appendPath(uri, k.value).toString() )
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

  val VersionMatcher = Segment.flatMap(s => VersionNumber.opt(s))


  def route(apiUser: GlobalPartyId, securedConnection: Boolean = true)(implicit ec: ExecutionContext, mat: ActorMaterializer) = {
    (handleRejections(VersionRejections.Handler) & handleExceptions(OcpiExceptionHandler.Default)) {
      extractUri { reqUri =>
        val uri = reqUri.withScheme(Uri.httpScheme(securedConnection))
        pathEndOrSingleSlash {
          versionsRoute(uri)
        } ~
        pathPrefix(VersionMatcher) { version =>
          val route = for {
            supportedVersion <- versions.get(version)
          } yield versionDetailsRoute(version, supportedVersion, uri, apiUser)
          route getOrElse reject(UnsupportedVersionRejection(version))
        }
      }
    }
  }
}
