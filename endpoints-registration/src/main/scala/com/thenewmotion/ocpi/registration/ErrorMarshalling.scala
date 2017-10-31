package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import common.EitherUnmarshalling
import RegistrationError._
import msgs.{ErrorResp, OcpiStatusCode}
import OcpiStatusCode._

object ErrorMarshalling extends EitherUnmarshalling {
  implicit def registrationErrorToResponseMarshaller(
    implicit mar: ToResponseMarshaller[(StatusCode, ErrorResp)]
  ): ToResponseMarshaller[RegistrationError] =
    mar.compose[RegistrationError] { e =>
      val (status, cec) = e match {
        case VersionsRetrievalFailed => (FailedDependency, UnableToUseApi)
        case VersionDetailsRetrievalFailed => (FailedDependency, UnableToUseApi)
        case SendingCredentialsFailed => (BadRequest, UnableToUseApi)
        case UpdatingCredentialsFailed => (BadRequest, UnableToUseApi)
        case SelectedVersionNotHostedByUs(_) => (BadRequest, UnsupportedVersion)
        case CouldNotFindMutualVersion => (BadRequest, UnsupportedVersion)
        case SelectedVersionNotHostedByThem(_) => (BadRequest, UnsupportedVersion)
        case RegistrationError.UnknownEndpointType(_) => (InternalServerError, OcpiStatusCode.UnknownEndpointType)
        case AlreadyExistingParty(_) => (MethodNotAllowed, PartyAlreadyRegistered)
        case UnknownParty(_) => (BadRequest, AuthenticationFailed)
        case WaitingForRegistrationRequest(_) => (MethodNotAllowed, RegistrationNotCompletedYetByParty)
        case CouldNotUnregisterParty(_) => (MethodNotAllowed, ClientWasNotRegistered)
      }

      (status, ErrorResp(cec, Some(e.reason)))
    }
}
