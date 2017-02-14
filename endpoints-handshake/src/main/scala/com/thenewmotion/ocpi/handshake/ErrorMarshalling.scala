package com.thenewmotion.ocpi.handshake

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import com.thenewmotion.ocpi.msgs.v2_1.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_1.OcpiStatusCode
import OcpiStatusCode._
import com.thenewmotion.ocpi.msgs.v2_1.OcpiJsonProtocol._
import com.thenewmotion.ocpi.common.DisjunctionMarshalling
import HandshakeError._

object ErrorMarshalling extends DisjunctionMarshalling {

  implicit val handshakeErrorToResponseMarshaller: ToResponseMarshaller[HandshakeError] =
    implicitly[ToResponseMarshaller[(StatusCode, ErrorResp)]]
    .compose[HandshakeError] { e =>
      val (status, cec) = e match {
        case VersionsRetrievalFailed => (FailedDependency, UnableToUseApi)
        case VersionDetailsRetrievalFailed => (FailedDependency, UnableToUseApi)
        case SendingCredentialsFailed => (BadRequest, UnableToUseApi)
        case SelectedVersionNotHostedByUs(v) => (BadRequest, UnsupportedVersion)
        case CouldNotFindMutualVersion => (BadRequest, UnsupportedVersion)
        case SelectedVersionNotHostedByThem(_) => (BadRequest, UnsupportedVersion)
        case HandshakeError.UnknownEndpointType(_) => (InternalServerError, OcpiStatusCode.UnknownEndpointType)
        case AlreadyExistingParty(p, c, v) => (Conflict, PartyAlreadyRegistered)
        case UnknownPartyToken => (BadRequest, AuthenticationFailed)
        case WaitingForRegistrationRequest => (BadRequest, RegistrationNotCompletedYetByParty)
      }

      (status, ErrorResp(cec, Some(e.reason)))
    }
}
