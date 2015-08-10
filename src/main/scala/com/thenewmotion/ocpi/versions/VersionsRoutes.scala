package com.thenewmotion.ocpi.versions

import com.thenewmotion.ocpi.msgs.v2_0.GenericSuccess
import com.thenewmotion.ocpi.{CurrentTimeComponent, UnknownVersion, UnknownVersionRejection}
import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.routing.HttpService
import scalaz.{\/-, -\/}

trait VersionsRoutes extends HttpService
                      with LazyLogging
                      with CurrentTimeComponent
                       {
  val vdh: VersionsDataHandler
  import spray.httpx.SprayJsonSupport._



  def versionsRoute = {
    import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._
    import com.thenewmotion.ocpi.msgs.v2_0.Versions._
    val versions = vdh.allVersions
        path(vdh.versionsNamespace) {
          get {
            complete(VersionsResp(
              GenericSuccess.code,
              GenericSuccess.default_message,
              currentTime.instance,
              versions.map { case (ver, url) => Version(ver, url) }.toList)
            )
          }
        } ~
        path(Segment) { version: String =>
          get {
            vdh.versionDetails(version)  match {
              case -\/(UnknownVersion) => reject(UnknownVersionRejection(version))
              case \/-(endpoints) => complete(
                VersionDetailsResp(
                  GenericSuccess.code,
                  GenericSuccess.default_message,
                  currentTime.instance,
                  VersionDetails(
                    version, endpoints.map { e =>
                    Endpoint(EndpointIdentifierEnum.withName(e.endpointType.name).get,  e.url)}))
                )
            }
          }
        }
  }
  
}





