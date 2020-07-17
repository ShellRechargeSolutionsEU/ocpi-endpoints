package com.thenewmotion.ocpi

import java.time.ZonedDateTime
import _root_.akka.http.scaladsl.model.Uri
import _root_.akka.http.scaladsl.server.{PathMatcher1, Route}
import cats.effect.Effect
import com.thenewmotion.ocpi
import com.thenewmotion.ocpi.VersionRejections._
import com.thenewmotion.ocpi.VersionsRoute.OcpiVersionConfig
import com.thenewmotion.ocpi.common._
import com.thenewmotion.ocpi.msgs.OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.Versions._
import com.thenewmotion.ocpi.msgs.{GlobalPartyId, SuccessResp, Url, Versions}

object VersionsRoute {

  def apply[F[_]: Effect: HktMarshallable](
    versions: => F[Map[VersionNumber, OcpiVersionConfig]]
  )(
    implicit successRespListVerM: SuccessRespMar[List[Versions.Version]],
    successVerDetM: SuccessRespMar[VersionDetails],
    errorM: ErrRespMar
  ): VersionsRoute[F] = new VersionsRoute(versions)

  case class OcpiVersionConfig(
    endPoints: Map[EndpointIdentifier, Either[Url, GuardedRoute]]
  )
}

class VersionsRoute[F[_]: Effect: HktMarshallable] private[ocpi](
  versions: => F[Map[VersionNumber, OcpiVersionConfig]]
)(
  implicit successRespListVerM: SuccessRespMar[List[Versions.Version]],
  successVerDetM: SuccessRespMar[VersionDetails],
  errorM: ErrRespMar
) extends OcpiDirectives {

  import VersionsRoute._

  private val EndPointPathMatcher = Segment.map(EndpointIdentifier(_))

  private def appendPath(uri: Uri, segments: String*) =
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) => path ?/ add
    })

  def versionsRoute(
    uri: Uri
  ): Route = onSuccess(Effect[F].toIO(versions).unsafeToFuture()) {
    case v if v.nonEmpty =>
      complete(
        SuccessResp(
          GenericSuccess,
          None,
          ZonedDateTime.now,
          v.keys.map(x => Version(x, Url(appendPath(uri, x.toString).toString))).toList
        )
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
          ZonedDateTime.now,
          VersionDetails(
            version,
            versionInfo.endPoints.map {
              case (k, Right(v))     => Endpoint(k, Url(appendPath(uri, k.value).toString))
              case (k, Left(extUri)) => Endpoint(k, extUri)
            }
          )
        )
      )
    } ~
      pathPrefix(EndPointPathMatcher) { path =>
        versionInfo.endPoints.get(path) match {
          case None               => reject
          case Some(Left(_))      => reject // implemented externally
          case Some(Right(route)) => route(version, apiUser)
        }
      }

  private val VersionMatcher: PathMatcher1[ocpi.Version] = Segment.flatMap(s => VersionNumber.opt(s))

  def apply(
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
          onSuccess(Effect[F].toIO(versions).unsafeToFuture()) { vers =>
            val route = for {
              supportedVersion <- vers.get(version)
            } yield versionDetailsRoute(version, supportedVersion, uri, apiUser)
            route.getOrElse(reject(UnsupportedVersionRejection(version)))
          }
        }
      }
    }
  }
}
