package com.thenewmotion.ocpi
package locations

import java.time.ZonedDateTime

import common.Pager
import common.PaginatedResult
import msgs.v2_1.Locations._

import scala.concurrent.Future
import scalaz._

trait CpoLocationsService {
  def locations(pager: Pager, dateFrom: Option[ZonedDateTime] = None, dateTo: Option[ZonedDateTime] = None): Future[LocationsError \/ PaginatedResult[Location]]

  def location(locId: String): Future[LocationsError \/ Location]

  def evse(locId: String, evseUid: String): Future[LocationsError \/ Evse]

  def connector(locId: String, evseUid: String, connectorId: String): Future[LocationsError \/ Connector]
}
