package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_0.Versions.EndpointIdentifier

case class OcpiVersionConfig(
  endPoints: Map[EndpointIdentifier, GuardedRoute]
)

case class OcpiRoutingConfig(
  namespace: String,
  versionsEndpoint: String,
  versions: Map[String, OcpiVersionConfig]
)(val authenticateApiUser: String => Option[ApiUser])
