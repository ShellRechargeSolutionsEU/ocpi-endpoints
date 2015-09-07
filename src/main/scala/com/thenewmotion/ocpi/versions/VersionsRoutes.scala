package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import spray.routing.HttpService
import scalaz.{\/-, -\/}

trait VersionsRoutes extends HttpService
                      with LazyLogging
                      with CurrentTimeComponent
                       {
  val vdh: VersionsDataHandler
  import spray.httpx.SprayJsonSupport._
  import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes._

  def versionsRoute = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Versions._
    path(vdh.versionsPath) {
      get {
        vdh.allVersions match {
          case \/-(versions) =>
            complete(VersionsResp(
              GenericSuccess.code,
              GenericSuccess.default_message,
              currentTime.instance,
              versions.map { case (ver, url) => Version(ver, url) }.toList)
            )
          case -\/(NoVersionsAvailable) => reject(NoVersionsRejection())
        }
      }
    }
  }
  def versionDetailsRoute(version: String) = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Versions._
    pathEndOrSingleSlash {
      get {
        vdh.versionDetails(version)  match {
          case \/-(endpoints) => complete(
            VersionDetailsResp(
              GenericSuccess.code,
              GenericSuccess.default_message,
              currentTime.instance,
              VersionDetails(
                version, endpoints.map { e =>
                Endpoint(EndpointIdentifierEnum.withName(e.endpointType.name).get,  e.url)}))
            )
          case -\/(UnknownVersion) => reject(UnsupportedVersionRejection(version))
          case _ => reject()
        }
      }
    }
  }
  
}





