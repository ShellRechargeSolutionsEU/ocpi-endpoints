package com.thenewmotion.ocpi

import spray.routing._

abstract class OcpiRestActor(routingConfig: OcpiRoutingConfig, statusChecks: List[StatusCheck])
  extends HttpServiceActor with TopLevelRoute {

  implicit private val rejectionHandler: RejectionHandler = OcpiRejectionHandler.Default

  import context.dispatcher

  override val statusRoute = new StatusRoute(statusChecks).route

  override def receive: Receive = runRoute(route )
}