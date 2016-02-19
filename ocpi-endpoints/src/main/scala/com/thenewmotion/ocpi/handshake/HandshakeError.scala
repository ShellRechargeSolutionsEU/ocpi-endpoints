package com.thenewmotion.ocpi.handshake

sealed trait HandshakeError {
  val reason: Option[String]
}

object HandshakeError{
  case class VersionsRetrievalFailed(
    reason: Option[String] = Some("Failed versions retrieval.")) extends HandshakeError
  case class VersionDetailsRetrievalFailed(
    reason: Option[String] = Some("Failed version details retrieval.")) extends HandshakeError
  case class SendingCredentialsFailed(
    reason: Option[String] = Some("Failed sending the credentials to connect to us.")) extends HandshakeError

  case class SelectedVersionNotHostedByUs(
    reason: Option[String] = Some("The selected version is not supported by our systems.")) extends HandshakeError
  case class CouldNotFindMutualVersion(
    reason: Option[String] = Some("Could not find mutual version.")) extends HandshakeError
  case class SelectedVersionNotHostedByThem(
    reason: Option[String] = Some("Selected version not supported by the requester party systems.")) extends HandshakeError
//  case class NoCredentialsEndpoint(
//    reason: Option[String] = Some("Credentials endpoint details required but not found.")) extends HandshakeError
  case class UnknownEndpointType(
    reason: Option[String] = Some(s"Unknown endpoint type.")) extends HandshakeError

  case class CouldNotPersistCredsForUs(
    reason: Option[String] = Some("Could not persist credentials sent to us.")) extends HandshakeError
  case class CouldNotPersistNewCredsForUs(
    reason: Option[String] = Some("Could not persist the new credentials sent to us.")) extends HandshakeError
  case class CouldNotPersistNewToken(
    reason: Option[String] = Some("Could not persist the new token.")) extends HandshakeError
  case class CouldNotPersistNewEndpoint(
    reason: Option[String] = Some("Could not persist new endpoint.")) extends HandshakeError
  case class CouldNotUpdateEndpoints(
    reason: Option[String] = Some("Could not update registered endpoints.")) extends HandshakeError
  case class CouldNotPersistNewParty(
    reason: Option[String] = Some("Could not persist new party.")) extends HandshakeError
  case class AlreadyExistingParty(
    reason: Option[String] = Some("Already existing partyId for this country and version.")) extends HandshakeError
  case class UnknownPartyToken(
    reason: Option[String] = Some("Unknown party token.")) extends HandshakeError
  case class WaitingForRegistrationRequest(
    reason: Option[String] = Some("Still waiting for registration request.")) extends HandshakeError
}