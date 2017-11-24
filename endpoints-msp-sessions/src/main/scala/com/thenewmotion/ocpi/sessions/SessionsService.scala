package com.thenewmotion.ocpi.sessions

import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionPatch}
import com.thenewmotion.ocpi.sessions.SessionError.IncorrectSessionId
import cats.syntax.either._
import cats.syntax.option._

import scala.concurrent.Future

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait SessionsService {

  protected[sessions] def createOrUpdateSession(
    apiUser: GlobalPartyId,
    sessionId: SessionId,
    session: Session
  ): Future[Either[SessionError, CreateOrUpdateResult]] = {
    if (session.id == sessionId) {
      createOrUpdateSession(apiUser, session)
    } else
      Future.successful(
        IncorrectSessionId(s"Session id from Url is $sessionId, but id in JSON body is ${session.id}".some).asLeft
      )
  }

  def createOrUpdateSession(
    globalPartyId: GlobalPartyId,
    session: Session
  ): Future[Either[SessionError, CreateOrUpdateResult]]

  def updateSession(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId,
    session: SessionPatch
  ): Future[Either[SessionError, Unit]]

  def session(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId
  ): Future[Either[SessionError, Session]]

}
