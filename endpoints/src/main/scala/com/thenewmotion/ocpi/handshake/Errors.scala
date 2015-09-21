package com.thenewmotion.ocpi.handshake

object Errors{
  sealed trait HandshakeError

  case object VersionsRetrievalFailed extends HandshakeError
  case object VersionDetailsRetrievalFailed extends HandshakeError
  case object SendingCredentialsFailed extends HandshakeError

  case object SelectedVersionNotHosted extends HandshakeError
  case object NoCredentialsEndpoint extends HandshakeError
  case object UnknownEndpointType extends HandshakeError
  case object CouldNotInsertEndpoint extends HandshakeError
  case object CouldNotRegisterParty extends HandshakeError

}