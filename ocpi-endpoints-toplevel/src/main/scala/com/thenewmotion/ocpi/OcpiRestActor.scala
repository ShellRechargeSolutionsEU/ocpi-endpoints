package com.thenewmotion.ocpi

import spray.routing._

abstract class OcpiRestActor(val routingConfig: OcpiRoutingConfig)
  extends HttpServiceActor with TopLevelRoute {

}