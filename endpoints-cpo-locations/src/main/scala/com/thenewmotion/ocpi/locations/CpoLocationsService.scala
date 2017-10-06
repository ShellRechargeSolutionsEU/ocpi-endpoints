package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import common.Pager
import common.PaginatedResult
import msgs.v2_1.Locations._

import scala.concurrent.Future

trait CpoLocationsService {
  def locations(
    pager: Pager,
    dateFrom: Option[ZonedDateTime] = None,
    dateTo: Option[ZonedDateTime] = None
  ): Future[Either[LocationsError, PaginatedResult[Location]]]

  def location(locId: LocationId): Future[Either[LocationsError, Location]]

  def evse(locId: LocationId, evseUid: EvseUid): Future[Either[LocationsError, Evse]]

  def connector(locId: LocationId, evseUid: EvseUid, connectorId: ConnectorId): Future[Either[LocationsError, Connector]]
}
