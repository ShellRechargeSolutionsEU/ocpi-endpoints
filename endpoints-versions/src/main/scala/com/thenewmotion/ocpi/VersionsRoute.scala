package com.thenewmotion.ocpi

import java.time.ZonedDateTime

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.Route
import VersionRejections._
import VersionsRoute.OcpiVersionConfig
import common.OcpiExceptionHandler
import msgs.{GlobalPartyId, SuccessWithDataResp, Url}
import msgs.OcpiStatusCode._
import msgs.Versions._

import scala.concurrent.Future

object VersionsRoute {
  case class OcpiVersionConfig(
    endPoints: Map[EndpointIdentifier, Either[Url, GuardedRoute]]
  )
}

class VersionsRoute(versions: => Future[Map[VersionNumber, OcpiVersionConfig]]) extends JsonApi {
  import VersionsRoute._
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def currentTime = ZonedDateTime.now

  val EndPointPathMatcher = Segment.map(EndpointIdentifier(_))

  def appendPath(uri: Uri, segments: String*) = {
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) if path.toString.endsWith("/") => path + add
      case (path, add) => path / add
    })
  }

  def versionsRoute(uri: Uri): Route = onSuccess(versions) {
    case v if v.nonEmpty =>
      complete(SuccessWithDataResp(
        GenericSuccess,
        None,
        currentTime,
        v.keys.map(x => Version(x, Url(appendPath(uri, x.toString).toString))).toList)
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
              case (k, Right(v)) => Endpoint(k, Url(appendPath(uri, k.value).toString))
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


  def route(apiUser: GlobalPartyId, securedConnection: Boolean = true) = {
    (handleRejections(VersionRejections.Handler) & handleExceptions(OcpiExceptionHandler.Default)) {
      extractUri { reqUri =>
        val uri = reqUri.withScheme(Uri.httpScheme(securedConnection))
        pathEndOrSingleSlash {
          versionsRoute(uri)
        } ~
        pathPrefix(VersionMatcher) { version =>
          onSuccess(versions){ vers =>
            val route = for {
              supportedVersion <- vers.get(version)
            } yield versionDetailsRoute(version, supportedVersion, uri, apiUser)
            route getOrElse reject(UnsupportedVersionRejection(version))
          }
        }
      }
    }
  }
}
