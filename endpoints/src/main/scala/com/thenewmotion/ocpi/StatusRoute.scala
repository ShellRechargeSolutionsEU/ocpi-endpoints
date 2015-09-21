package com.thenewmotion.ocpi

import spray.http.{HttpResponse, StatusCodes}
import spray.routing._

trait StatusRoute extends HttpService {
  lazy val statusRoute: Route = get {
    (path("status") & pathEnd) {
      complete {
          HttpResponse(StatusCodes.OK, "OK")
      }
    }
  }
}
