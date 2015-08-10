package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.versions.VersionsRoutes
import spray.routing._
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{ExecutionContext, Future}

abstract class OcpiRestActor extends HttpServiceActor with TopLevelRoutes {

  //implicit private val rejectionHandler: RejectionHandler = ???

  override def receive: Receive =
    runRoute(allRoutes )
}

trait TopLevelRoutes extends HttpService with VersionsRoutes with Authenticator with CurrentTimeComponent{
  import scala.concurrent.ExecutionContext.Implicits.global
  val tldh: TopLevelRouteDataHanlder
  val currentTime = new CurrentTime
  def allRoutes =
    authenticate(basicUserAuthenticator) { _ =>
      pathPrefix(tldh.namespace) {
//        wrapInRespObj {
          versionsRoute
//        }
      }
    }
}

//case class Resp(status_message: String, data: String)

//trait ResponseWrapper {
//  import Directives.mapHttpResponseEntity
//  import spray.json._
//  import OcpiJsonProtocol._
//  import spray.httpx.SprayJsonSupport._
//
//  implicit val respFormat = jsonFormat2(Resp)
//  def wrapEntity(entity: HttpEntity): HttpEntity = {
//    val resp = Resp(status_message = "success", data = entity.data.asString.parseJson.prettyPrint)
//    HttpEntity(resp.toJson.toString())
//  }
//  val wrapInRespObj: Directive0 = mapHttpResponseEntity(wrapEntity)
//}

trait Authenticator {
  val adh: AuthDataHandler
  import adh._

  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[ApiUser] = {
    def validateUser(userPass: Option[UserPass]): Option[ApiUser] = {
      for {
        up <- userPass
        au <- apiuser(up.pass)
      } yield au
    }

    def authenticator(userPass: Option[UserPass]): Future[Option[ApiUser]] =
      Future { validateUser(userPass) }

    BasicAuth(authenticator _, realm = "Private OCPI API")
  }
}

trait CurrentTimeComponent {
  import org.joda.time.DateTime
  val currentTime: CurrentTime
  class CurrentTime {
    def instance = DateTime.now()
  }
}