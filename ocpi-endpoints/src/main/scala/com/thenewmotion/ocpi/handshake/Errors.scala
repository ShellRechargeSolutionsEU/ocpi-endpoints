package com.thenewmotion.ocpi.handshake

object Errors{
  sealed trait HandshakeError
  sealed trait PersistenceError extends HandshakeError

  case object VersionsRetrievalFailed extends HandshakeError
  case object VersionDetailsRetrievalFailed extends HandshakeError
  case object SendingCredentialsFailed extends HandshakeError

  case object SelectedVersionNotHosted extends HandshakeError
  case object NoCredentialsEndpoint extends HandshakeError
  case object UnknownEndpointType extends HandshakeError
  case object CouldNotInsertEndpoint extends HandshakeError
  case object CouldNotRegisterParty extends HandshakeError

  case object CouldNotPersistPreferences extends PersistenceError
  case object CouldNotPersistNewToken extends PersistenceError
  case object CouldNotPersistEndpoint extends PersistenceError
}