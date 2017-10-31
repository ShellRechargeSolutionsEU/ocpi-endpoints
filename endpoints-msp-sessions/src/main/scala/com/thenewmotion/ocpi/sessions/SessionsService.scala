package com.thenewmotion.ocpi.sessions

import com.thenewmotion.ocpi.common.CreateOrUpdateResult
import com.thenewmotion.ocpi.msgs.GlobalPartyId
import com.thenewmotion.ocpi.msgs.v2_1.Sessions.{Session, SessionId, SessionPatch}

import scala.concurrent.{ExecutionContext, Future}

/**
  * All methods are to be implemented in an idempotent fashion.
  */
trait SessionsService {

  def createOrUpdateSession(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId,
    session: Session
  )(implicit ec: ExecutionContext): Future[Either[SessionError, CreateOrUpdateResult]]

  def updateSession(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId,
    session: SessionPatch
  )(implicit ec: ExecutionContext): Future[Either[SessionError, Unit]]

  def session(
    globalPartyId: GlobalPartyId,
    sessionId: SessionId
  )(implicit ec: ExecutionContext): Future[Either[SessionError, Session]]

}
