package com.thenewmotion.ocpi
package registration

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import msgs.v2_1.OcpiJsonProtocol._
import common.DisjunctionMarshalling
import RegistrationError._
import msgs.{ErrorResp, OcpiStatusCode}
import OcpiStatusCode._

object ErrorMarshalling extends DisjunctionMarshalling {

  implicit val registrationErrorToResponseMarshaller: ToResponseMarshaller[RegistrationError] =
    implicitly[ToResponseMarshaller[(StatusCode, ErrorResp)]]
    .compose[RegistrationError] { e =>
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
