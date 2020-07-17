package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import common.Pager
import common.PaginatedResult
import msgs.v2_1.Locations._


trait CpoLocationsService[F[_]] {
  def locations(
    pager: Pager,
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  ): F[Either[LocationsError, PaginatedResult[Location]]]

  def location(locId: LocationId): F[Either[LocationsError, Location]]

  def evse(locId: LocationId, evseUid: EvseUid): F[Either[LocationsError, Evse]]

  def connector(locId: LocationId, evseUid: EvseUid, connectorId: ConnectorId): F[Either[LocationsError, Connector]]
}
