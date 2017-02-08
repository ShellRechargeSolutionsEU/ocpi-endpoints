package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.msgs.v2_1.Versions.VersionNumber

sealed abstract class HandshakeError(val reason: String)

object HandshakeError{
  case object VersionsRetrievalFailed extends HandshakeError(
    "Failed versions retrieval.")

  case object VersionDetailsRetrievalFailed extends HandshakeError(
    "Failed version details retrieval.")

  case object SendingCredentialsFailed extends HandshakeError(
    "Failed sending the credentials to connect to us.")

  case class SelectedVersionNotHostedByUs(version: VersionNumber) extends HandshakeError(
    s"The selected version: ${version.name}, is not supported by our systems.")

  case object CouldNotFindMutualVersion extends HandshakeError(
    "Could not find mutual version.")

  case class SelectedVersionNotHostedByThem(version: VersionNumber) extends HandshakeError(
    s"Selected version: ${version.name}, not supported by the requester party systems.")

  case class UnknownEndpointType(endpointType: String) extends HandshakeError(
    s"Unknown endpoint type: $endpointType")

  case object CouldNotPersistCredsForUs extends HandshakeError(
    "Could not persist credentials sent to us.")

  case object CouldNotPersistNewCredsForUs extends HandshakeError(
    "Could not persist the new credentials sent to us.")

  case class CouldNotPersistNewToken(newToken: String) extends HandshakeError(
    s"Could not persist the new token: $newToken.")

  case class CouldNotPersistNewEndpoint(endpoint: String) extends HandshakeError(
    s"Could not persist new endpoint: $endpoint.")

  case object CouldNotUpdateEndpoints extends HandshakeError(
    "Could not update registered endpoints.")

  case class CouldNotPersistNewParty(partyId: String) extends HandshakeError(
    s"Could not persist new party: $partyId.")

  case class AlreadyExistingParty(partyId: String, country: String, version: VersionNumber) extends HandshakeError(
    s"Already existing partyId: '$partyId' for country: '$country' and version: '${version.name}'.")

  case class UnknownPartyToken(token: String) extends HandshakeError(
    s"Unknown party token: '$token'.")

  case object WaitingForRegistrationRequest extends HandshakeError(
    "Still waiting for registration request.")

}
