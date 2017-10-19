package com.thenewmotion.ocpi

import java.time.ZonedDateTime

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.server.{PathMatcher1, Route}
import VersionRejections._
import VersionsRoute.OcpiVersionConfig
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import com.thenewmotion.ocpi
import common.OcpiExceptionHandler
import msgs.{ErrorResp, GlobalPartyId, SuccessResp, Url, Versions}
import msgs.OcpiStatusCode._
import msgs.Versions._
import scala.concurrent.Future

object VersionsRoute {
  case class OcpiVersionConfig(
    endPoints: Map[EndpointIdentifier, Either[Url, GuardedRoute]]
  )
}

class VersionsRoute(
  versions: => Future[Map[VersionNumber, OcpiVersionConfig]]
)(
  implicit successRespListVerM: ToEntityMarshaller[SuccessResp[List[Versions.Version]]],
  successVerDetM: ToEntityMarshaller[SuccessResp[VersionDetails]],
  errorM: ToEntityMarshaller[ErrorResp]
) extends JsonApi {

  import VersionsRoute._

  protected def currentTime = ZonedDateTime.now

  private val EndPointPathMatcher = Segment.map(EndpointIdentifier(_))

  private def appendPath(uri: Uri, segments: String*) = {
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) if path.toString.endsWith("/") => path + add
      case (path, add) => path / add
    })
  }

  def versionsRoute(
    uri: Uri
  ): Route = onSuccess(versions) {
    case v if v.nonEmpty =>
      complete(SuccessResp(
        GenericSuccess,
        None,
        currentTime,
        v.keys.map(x => Version(x, Url(appendPath(uri, x.toString).toString))).toList)
      )
    case _ => reject(NoVersionsRejection())
  }

  def versionDetailsRoute(
    version: VersionNumber,
    versionInfo: OcpiVersionConfig,
    uri: Uri,
    apiUser: GlobalPartyId
  ): Route =
    pathEndOrSingleSlash {
      complete(
        SuccessResp(
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

  private val VersionMatcher: PathMatcher1[ocpi.Version] = Segment.flatMap(s => VersionNumber.opt(s))

  def route(
    apiUser: GlobalPartyId,
    securedConnection: Boolean = true
  ): Route = {
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
