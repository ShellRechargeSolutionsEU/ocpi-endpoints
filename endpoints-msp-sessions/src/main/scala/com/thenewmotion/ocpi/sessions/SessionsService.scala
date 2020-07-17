package com.thenewmotion.ocpi.sessions

import cats.Applicative
import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionPatch}
import com.thenewmotion.ocpi.sessions.SessionError.IncorrectSessionId
import cats.syntax.either._
import cats.syntax.option._

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait SessionsService[F[_]] {

  protected[sessions] def createOrUpdateSession(
    apiUser: GlobalPartyId,
    sessionId: SessionId,
    session: Session
  )(
    implicit A: Applicative[F]
  ): F[Either[SessionError, CreateOrUpdateResult]] = {
    if (session.id == sessionId) {
      createOrUpdateSession(apiUser, session)
    } else
      Applicative[F].pure(
        IncorrectSessionId(s"Session id from Url is $sessionId, but id in JSON body is ${session.id}".some).asLeft
      )
  }

  def createOrUpdateSession(
    globalPartyId: GlobalPartyId,
    session: Session
  ): F[Either[SessionError, CreateOrUpdateResult]]

  def updateSession(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId,
    session: SessionPatch
  ): F[Either[SessionError, Unit]]

  def session(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId
  ): F[Either[SessionError, Session]]

}
