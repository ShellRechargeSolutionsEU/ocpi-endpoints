package com.thenewmotion.ocpi.credentials

sealed trait CredentialsError
sealed trait RegistrationError extends CredentialsError
sealed trait ListError extends CredentialsError


case object CouldNotRegisterParty extends RegistrationError

