package com.thenewmotion.ocpi
package handshake

import msgs.Versions.VersionNumber
import msgs.v2_1.CommonTypes.{CountryCode, PartyId}

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

  case class AlreadyExistingParty(partyId: PartyId, country: CountryCode, version: VersionNumber) extends HandshakeError(
    s"Already existing partyId: '$partyId' for country: '$country' and version: '${version.name}'.")

  case object UnknownPartyToken extends HandshakeError("Unknown party token")

  case object WaitingForRegistrationRequest extends HandshakeError(
    "Still waiting for registration request.")

}
