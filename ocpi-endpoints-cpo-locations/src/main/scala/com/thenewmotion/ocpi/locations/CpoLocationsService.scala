package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_1.Locations._
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz._


trait CpoLocationsService {

  def locations(pager: Pager, dateFrom: Option[DateTime] = None, dateTo: Option[DateTime] = None):
    Future[LocationsError \/ PaginatedResult[Location]]

  def location(locId: String): Future[LocationsError \/ Location]

  def evse(locId: String, evseUid: String): Future[LocationsError \/ Evse]

  def connector(locId: String, evseUid: String, connectorId: String): Future[LocationsError \/ Connector]
}
