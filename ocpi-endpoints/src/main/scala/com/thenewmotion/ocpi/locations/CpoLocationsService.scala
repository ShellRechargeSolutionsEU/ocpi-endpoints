package com.thenewmotion.ocpi.locations

import com.thenewmotion.ocpi.common.{Pager, PaginatedResult}
import com.thenewmotion.ocpi.msgs.v2_0.Locations._
import scala.concurrent.Future
import scalaz._


trait CpoLocationsService {

  def locations(pager: Pager): Future[LocationsError \/ PaginatedResult[Location]]

}
