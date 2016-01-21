package com.thenewmotion.ocpi.handshake

object Errors{
  sealed trait HandshakeError

  case object VersionsRetrievalFailed extends HandshakeError
  case object VersionDetailsRetrievalFailed extends HandshakeError
  case object SendingCredentialsFailed extends HandshakeError

  case object SelectedVersionNotHostedByUs extends HandshakeError
  case object CouldNotFindMutualVersion extends HandshakeError
  case object SelectedVersionNotHostedByThem extends HandshakeError
  case object NoCredentialsEndpoint extends HandshakeError
  case object UnknownEndpointType extends HandshakeError
  case object CouldNotInsertEndpoint extends HandshakeError

  case object CouldNotFindEndpoint extends HandshakeError

  case object CouldNotPersistPreferences extends HandshakeError
  case object CouldNotPersistNewToken extends HandshakeError
  case object CouldNotPersistNewEndpoint extends HandshakeError
  case object CouldNotPersistNewParty extends HandshakeError
  case object AlreadyExistingParty extends HandshakeError
  case object UnknownPartyToken extends HandshakeError
  case object WaitingForRegistrationRequest extends HandshakeError
}