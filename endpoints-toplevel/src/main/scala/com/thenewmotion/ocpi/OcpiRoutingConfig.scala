package com.thenewmotion.ocpi

import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Credentials.TheirToken
import handshake.HandshakeService
import msgs.v2_1.Versions.{EndpointIdentifier, VersionNumber}
import scala.concurrent.Future

case class OcpiVersionConfig(
  endPoints: Map[EndpointIdentifier, Either[URI, GuardedRoute]]
)

case class OcpiRoutingConfig(
  namespace: String,
  versions: Map[VersionNumber, OcpiVersionConfig],
  handshakeService: HandshakeService
)(val authenticateApiUser: TheirToken => Future[Option[GlobalPartyId]]
)(val authenticateInternalUser: TheirToken => Future[Option[GlobalPartyId]])
