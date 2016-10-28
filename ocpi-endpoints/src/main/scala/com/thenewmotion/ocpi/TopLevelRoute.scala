package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.handshake.InitiateHandshakeRoute
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCodes.GenericSuccess
import com.thenewmotion.ocpi.msgs.v2_1.Versions._
import spray.http._, HttpHeaders._
import spray.routing._, authentication._
import scala.concurrent.{ExecutionContext, Future}
import org.joda.time.DateTime
import spray.http.Uri


trait TopLevelRoute extends JsonApi {
  import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._

  def routingConfig: OcpiRoutingConfig

  def currentTime = DateTime.now

  val EndPointPathMatcher = Segment.flatMap {
    case s => EndpointIdentifier.withName(s)
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
        Some(GenericSuccess.defaultMessage),
        currentTime,
        v.keys.flatMap(x => VersionNumber.withName(x).map(Version(_, appendPath(uri, x).toString()))).toList)
      )
    case _ => reject(NoVersionsRejection())
  }

  def versionDetailsRoute(version: VersionNumber, versionInfo: OcpiVersionConfig, uri: Uri, apiUser: ApiUser): Route =
    pathEndOrSingleSlash {
      complete(
        VersionDetailsResp(
          GenericSuccess.code,
          Some(GenericSuccess.defaultMessage),
          currentTime,
          VersionDetails(
            version, versionInfo.endPoints.map {
              case (k, Right(v)) => Endpoint(k, appendPath(uri, k.name).toString() )
              case (k, Left(extUri)) => Endpoint(k, extUri)
            }.toList
          )
        )
      )
    } ~
    pathPrefix(EndPointPathMatcher) { path =>
      versionInfo.endPoints.get(path) match {
        case None => reject
        case Some(Left(extUri)) => reject // implemented externally
        case Some(Right(route)) => route(version, apiUser)
      }
    }


  def topLevelRoute(implicit ec: ExecutionContext) = {
    val externalUseToken = new TokenAuthenticator(routingConfig.authenticateApiUser)
    val internalUseToken = new TokenAuthenticator(routingConfig.authenticateInternalUser)

    (pathPrefix(routingConfig.namespace) & extract(_.request.uri)) { uri =>
      pathPrefix("initiateHandshake") {
        pathEndOrSingleSlash {
          authenticate(internalUseToken) { internalUser: ApiUser =>
            new InitiateHandshakeRoute(routingConfig.handshakeService).route
          }
        }
      } ~
      authenticate(externalUseToken) { apiUser: ApiUser =>
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

class TokenAuthenticator(
  apiUser: String => Option[ApiUser]
)(implicit val executionContext: ExecutionContext) extends HttpAuthenticator[ApiUser] {

  val challenge = `WWW-Authenticate`(
    HttpChallenge(scheme = "Token", realm = "ocpi", params = Map.empty)) :: Nil

  def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext) =
    Future(
      credentials
        .flatMap {
          case GenericHttpCredentials("Token", token, _) => Some(token)
          case _ => None
        }
        .flatMap(apiUser)
    )

  def getChallengeHeaders(r: HttpRequest) = challenge
}
