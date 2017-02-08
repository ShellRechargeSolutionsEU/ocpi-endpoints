package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.handshake.HandshakeService
import com.thenewmotion.ocpi.msgs.v2_1.Versions.EndpointIdentifier
import scala.concurrent.Future

case class OcpiVersionConfig(
  endPoints: Map[EndpointIdentifier, Either[URI, GuardedRoute]]
)

case class OcpiRoutingConfig(
  namespace: String,
  versions: Map[String, OcpiVersionConfig],
  handshakeService: HandshakeService
)(val authenticateApiUser: String => Future[Option[ApiUser]]
)(val authenticateInternalUser: String => Future[Option[ApiUser]])
