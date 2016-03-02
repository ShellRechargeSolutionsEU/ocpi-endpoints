package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.handshake.{HandshakeErrorRejection, HandshakeError, InitiateHandshakeRoute}
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_0.Versions._
import spray.routing._
import spray.routing.authentication.Authentication
import spray.routing.directives.FutureDirectives
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import spray.http.Uri

import scalaz.{\/-, -\/, \/}

trait TopLevelRoute extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  def routingConfig: OcpiRoutingConfig

  def currentTime = DateTime.now

  lazy val externalUseToken = new Authenticator(routingConfig.authenticateApiUser)
  lazy val internalUseToken = new Authenticator(routingConfig.authenticateInternalUser)

  val EndPointPathMatcher = Segment.flatMap {
    case s => EndpointIdentifier.withName(s)
  }

  trait TopLevelApi extends JsonApi {
    protected def leftToRejection[T](errOrX: HandshakeError \/ T)(f: T => Route): Route =
      errOrX match {
        case -\/(err) => reject(HandshakeErrorRejection(err))
        case \/-(res) => f(res)
      }

    protected def futLeftToRejection[T](errOrX: Future[HandshakeError \/ T])(f: T => Route)
      (implicit ec: ExecutionContext): Route = {
      FutureDirectives.onSuccess(errOrX) {
        case -\/(err) => reject(HandshakeErrorRejection(err))
        case \/-(res) => f(res)
      }
    }
  }

  def appendPath(uri: Uri, segments: String*) = {
    uri.withPath(segments.foldLeft(uri.path) {
      case (path, add) if path.toString().endsWith("/") => path + add
      case (path, add) => path / add
    })
  }

  def versionsRoute(uri: Uri): Route = routingConfig.versions match {
    case v if v.nonEmpty =>
      complete(VersionsResp(
        GenericSuccess.code,
        Some(GenericSuccess.default_message),
        currentTime,
        v.keys.map(x => Version(x, appendPath(uri, x).toString())).toList)
      )
    case _ => reject(NoVersionsRejection())
  }

  def versionRoute(version: String, versionInfo: OcpiVersionConfig, uri: Uri, apiUser: ApiUser): Route =
    pathEndOrSingleSlash {
      complete(
        VersionDetailsResp(
          GenericSuccess.code,
          Some(GenericSuccess.default_message),
          currentTime,
          VersionDetails(
            version, versionInfo.endPoints.map {
              case (k, v) => Endpoint(k, appendPath(uri, k.name).toString() )
            }.toList
          )
        )
      )
    } ~
    pathPrefix(EndPointPathMatcher) { path =>
      versionInfo.endPoints.get(path) match {
        case None => reject
        case Some(route) => route(version, apiUser.token)
      }
    }


  def topLevelRoute(implicit ec: ExecutionContext) =
    headerValueByName("Authorization") { access_token =>
      (pathPrefix(routingConfig.namespace) & extract(_.request.uri)) { uri =>

        pathPrefix("initiateHandshake") {
          pathEndOrSingleSlash {
            authenticate(internalUseToken.validate(access_token)) { internalUser: ApiUser =>
              new InitiateHandshakeRoute(routingConfig.handshakeService).routeWithoutRH
            }
          }
        } ~
        authenticate(externalUseToken.validate(access_token)) { apiUser: ApiUser =>
          pathPrefix(EndpointIdentifier.Versions.name) {
            pathEndOrSingleSlash {
              versionsRoute(uri)
            } ~
            pathPrefix(Segment) { version =>
              routingConfig.versions.get(version) match {
                case None => reject(UnsupportedVersionRejection(version))
                case Some(validVersion) => versionRoute(version, validVersion, uri, apiUser)
              }
            }
          }
        }

      }
    }
}

class Authenticator(authenticateApiUser: String => Option[ApiUser]) {

  val logger = Logger(getClass)

  def validate(token: String)(implicit ec: ExecutionContext): Future[Authentication[ApiUser]] = {
    Future {
      extractTokenValue(token) match {
        case Some(tokenVal) => authenticateApiUser(tokenVal)
          .toRight(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
        case None => Left(AuthenticationFailedRejection(AuthenticationFailedRejection.CredentialsRejected, List()))
      }
    }
  }

  def extractTokenValue(token: String)(implicit ec: ExecutionContext): Option[String] = {
    val authScheme = "Token "
    if (token.startsWith(authScheme)){
      val tokenVal = token.substring(authScheme.length)
      if (!tokenVal.isEmpty) Some(tokenVal) else None
    }
    else {
      logger.debug(s"Auth failed for token beginning with ${token.substring(0,2)}")
      None
    }
  }
}
