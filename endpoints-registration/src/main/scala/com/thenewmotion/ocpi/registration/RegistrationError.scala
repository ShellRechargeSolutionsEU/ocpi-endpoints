package com.thenewmotion.ocpi
package registration

import msgs.Versions.VersionNumber
import msgs.GlobalPartyId

sealed abstract class RegistrationError(val reason: String)

object RegistrationError{
  case object VersionsRetrievalFailed extends RegistrationError(
    "Failed versions retrieval.")

  case object VersionDetailsRetrievalFailed extends RegistrationError(
    "Failed version details retrieval.")

  case object SendingCredentialsFailed extends RegistrationError(
    "Failed sending the credentials to connect to us.")

  case class SelectedVersionNotHostedByUs(version: VersionNumber) extends RegistrationError(
    s"The selected version: $version, is not supported by our systems.")

  case object CouldNotFindMutualVersion extends RegistrationError(
    "Could not find mutual version.")

  case class SelectedVersionNotHostedByThem(version: VersionNumber) extends RegistrationError(
    s"Selected version: $version, not supported by the requester party systems.")

  case class UnknownEndpointType(endpointType: String) extends RegistrationError(
    s"Unknown endpoint type: $endpointType")

  case class AlreadyExistingParty(globalPartyId: GlobalPartyId) extends RegistrationError(
    s"Already existing global partyId: '$globalPartyId'")

  case class UnknownParty(globalPartyId: GlobalPartyId) extends RegistrationError(
    s"Unknown global partyId: '$globalPartyId")

  case class WaitingForRegistrationRequest(globalPartyId: GlobalPartyId) extends RegistrationError(
    "Still waiting for registration request.")

  case class CouldNotUnregisterParty(globalPartyId: GlobalPartyId) extends RegistrationError(
    s"Client is not registered for partyId: $globalPartyId")
}
