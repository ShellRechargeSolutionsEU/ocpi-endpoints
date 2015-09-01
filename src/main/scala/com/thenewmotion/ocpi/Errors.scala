package com.thenewmotion.ocpi

sealed trait Error
sealed trait ListError extends Error
sealed trait CreateError extends Error

// can only inherit from sealed trait if in same file...
case object UnknownVersion extends ListError
case object NoVersionsAvailable extends ListError
case object CouldNotRegisterToken extends CreateError

