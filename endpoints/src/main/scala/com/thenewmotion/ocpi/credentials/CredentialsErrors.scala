package com.thenewmotion.ocpi.credentials

object CredentialsErrors{
  sealed trait CredentialsError
  sealed trait RegistrationError extends CredentialsError
  sealed trait ListError extends CredentialsError
  sealed trait AuthError
  case object VersionsRetrievalFailed extends RegistrationError
  case object SelectedVersionNotHosted extends RegistrationError
  case object VersionDetailsRetrievalFailed extends RegistrationError
  case object NoCredentialsEndpoint extends RegistrationError
  case object SendingCredentialsFailed extends RegistrationError
  case object UnknownEndpointType extends RegistrationError
  case object CouldNotInsertEndpoint extends RegistrationError

  case object UnknownParty extends AuthError

  case object CouldNotRegisterParty extends RegistrationError

}