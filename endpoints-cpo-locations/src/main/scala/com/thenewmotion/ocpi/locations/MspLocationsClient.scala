package com.thenewmotion.ocpi.locations

import scala.concurrent.{ExecutionContext}
import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.stream.Materializer
import cats.effect.{ContextShift, IO}
import com.thenewmotion.ocpi.common.{ClientObjectUri, ErrRespUnMar, OcpiClient, SuccessRespUnMar}
import com.thenewmotion.ocpi.msgs.AuthToken
import com.thenewmotion.ocpi.msgs.Ownership.Ours
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import cats.syntax.either._

class MspLocationsClient(
  implicit http: HttpExt,
  successUnitU: SuccessRespUnMar[Unit],
  errorU: ErrRespUnMar,
  successLocU: SuccessRespUnMar[Location],
  successEvseU: SuccessRespUnMar[Evse],
  successConnU: SuccessRespUnMar[Connector],
  locationM: ToEntityMarshaller[Location],
  evseM: ToEntityMarshaller[Evse],
  connectorM: ToEntityMarshaller[Connector],
  locationPM: ToEntityMarshaller[LocationPatch],
  evsePM: ToEntityMarshaller[EvsePatch],
  connectorPM: ToEntityMarshaller[ConnectorPatch]
) extends OcpiClient {

  private def get[T](
    uri: ClientObjectUri,
    authToken: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer,
    successU: SuccessRespUnMar[T]
  ): IO[ErrorRespOr[T]] =
    singleRequest[T](Get(uri.value), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not retrieve data from ${uri.value}. Reason: $err")
        err
      }, _.data)
    }

  private def upload[T: ToEntityMarshaller](
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    data: T
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    singleRequest[Unit](Put(uri.value, data), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not upload data to ${uri.value}. Reason: $err")
        err
      }, _ => ())
    }

  private def update[T: ToEntityMarshaller](
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    patch: T
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    singleRequest[Unit](Patch(uri.value, patch), authToken).map {
      _.bimap(err => {
        logger.error(s"Could not update data at ${uri.value}. Reason: $err")
        err
      }, _ => ())
    }

  def getLocation(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Location]] =
    get(uri, authToken)

  def getEvse(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Evse]] =
    get(uri, authToken)

  def getConnector(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours]
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Connector]] =
    get(uri, authToken)

  def uploadLocation(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    location: Location
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    upload(uri, authToken, location)

  def uploadEvse(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    evse: Evse
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    upload(uri, authToken, evse)

  def uploadConnector(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    connector: Connector
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    upload(uri, authToken, connector)

  def updateLocation(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    location: LocationPatch
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    update(uri, authToken, location)

  def updateEvse(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    evse: EvsePatch
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    update(uri, authToken, evse)

  def updateConnector(
    uri: ClientObjectUri,
    authToken: AuthToken[Ours],
    connector: ConnectorPatch
  )(
    implicit ec: ExecutionContext,
    cs: ContextShift[IO],
    mat: Materializer
  ): IO[ErrorRespOr[Unit]] =
    update(uri, authToken, connector)
}
