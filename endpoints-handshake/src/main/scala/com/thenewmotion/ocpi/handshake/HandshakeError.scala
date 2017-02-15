package com.thenewmotion.ocpi
package handshake

import msgs.Versions.VersionNumber
import msgs.GlobalPartyId

sealed abstract class HandshakeError(val reason: String)

object HandshakeError{
  case object VersionsRetrievalFailed extends HandshakeError(
    "Failed versions retrieval.")

  case object VersionDetailsRetrievalFailed extends HandshakeError(
    "Failed version details retrieval.")

  case object SendingCredentialsFailed extends HandshakeError(
    "Failed sending the credentials to connect to us.")

  case class SelectedVersionNotHostedByUs(version: VersionNumber) extends HandshakeError(
    s"The selected version: $version, is not supported by our systems.")

  case object CouldNotFindMutualVersion extends HandshakeError(
    "Could not find mutual version.")

  case class SelectedVersionNotHostedByThem(version: VersionNumber) extends HandshakeError(
    s"Selected version: $version, not supported by the requester party systems.")

  case class UnknownEndpointType(endpointType: String) extends HandshakeError(
    s"Unknown endpoint type: $endpointType")

  case class AlreadyExistingParty(globalPartyId: GlobalPartyId) extends HandshakeError(
    s"Already existing global partyId: '$globalPartyId'")

  case class UnknownParty(globalPartyId: GlobalPartyId) extends HandshakeError(
    s"Unknown global partyId: '$globalPartyId")

  case object WaitingForRegistrationRequest extends HandshakeError(
    "Still waiting for registration request.")

}
