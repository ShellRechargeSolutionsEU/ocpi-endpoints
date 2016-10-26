package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import org.joda.time.DateTime
import scala.concurrent.Future
import scalaz._


trait CpoLocationsService {

  def locations(pager: Pager, date_from: Option[DateTime] = None, date_to: Option[DateTime] = None): Future[LocationsError \/ PaginatedResult[Location]]

}
