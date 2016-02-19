package com.thenewmotion.ocpi.handshake

import com.thenewmotion.ocpi.handshake.HandshakeError._
import com.thenewmotion.ocpi.msgs.v2_0.CommonTypes.ErrorResp
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes
import com.thenewmotion.ocpi.msgs.v2_0.OcpiStatusCodes._
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing._
import spray.routing.directives.{MiscDirectives, BasicDirectives}
import spray.routing.directives.RouteDirectives._

object HandshakeRejectionHandler  extends BasicDirectives with MiscDirectives with SprayJsonSupport {

  import com.thenewmotion.ocpi.msgs.v2_0.OcpiJsonProtocol._

  val Default = RejectionHandler {

    // UnableToUseApi
    case HandshakeErrorRejection(e@VersionsRetrievalFailed(reason)) :: _ => complete {
      ( FailedDependency,
        ErrorResp(
          UnableToUseApi.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@VersionDetailsRetrievalFailed(reason)) :: _ => complete {
      ( FailedDependency,
        ErrorResp(
          UnableToUseApi.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    // Initiate handshake specific error
    case HandshakeErrorRejection(e@SendingCredentialsFailed(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnableToUseApi.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    // UnsupportedVersion
    case HandshakeErrorRejection(e@SelectedVersionNotHostedByUs(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnsupportedVersion.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotFindMutualVersion(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnsupportedVersion.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@SelectedVersionNotHostedByThem(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          UnsupportedVersion.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    // Endpoints
    //TODO: TNM-2013: It doesn't work yet, it must be used to fail with that error when required endpoints not included
    case (r@ValidationRejection(msg, cause)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          MissingExpectedEndpoints.code,
          Some(s"${MissingExpectedEndpoints.default_message} $msg"),
          DateTime.now()).toJson.compactPrint)
    }

    // Is recognized by OCPI msgs but not internally by the application that uses it
    case HandshakeErrorRejection(e@HandshakeError.UnknownEndpointType(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          OcpiStatusCodes.UnknownEndpointType.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    // GenericServerFailure
    case HandshakeErrorRejection(e@CouldNotPersistCredsForUs(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotPersistCredsForUs(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotPersistNewCredsForUs(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotPersistNewToken(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotPersistNewEndpoint(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotUpdateEndpoints(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@CouldNotPersistNewParty(reason)) :: _ => complete {
      ( InternalServerError,
        ErrorResp(
          GenericServerFailure.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    // Not allowed
    case HandshakeErrorRejection(e@AlreadyExistingParty(reason)) :: _ => complete {
      ( Conflict,
        ErrorResp(
          PartyAlreadyRegistered.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@UnknownPartyToken(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          AuthenticationFailed.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

    case HandshakeErrorRejection(e@WaitingForRegistrationRequest(reason)) :: _ => complete {
      ( BadRequest,
        ErrorResp(
          RegistrationNotCompletedYetByParty.code,
          reason,
          DateTime.now()).toJson.compactPrint)
    }

  }
}
