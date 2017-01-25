package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Link, LinkParams, RawHeader}
import akka.http.scaladsl.server.{Directive0, Directives}
import org.joda.time.DateTime

case class PaginatedResult[+T](result: Iterable[T], total: Int)

case class Pager(offset: Int, limit: Int) {
  def nextOffset = offset + limit
}

trait PaginatedRoute extends Directives with DateDeserializers {

  private val DefaultOffset = 0

  def DefaultLimit: Int
  def MaxLimit: Int

  private val offset = parameter('offset.as[Int] ? DefaultOffset)

  private val limit = parameter('limit.as[Int] ? DefaultLimit).tmap {
    case Tuple1(l) => Math.min(l, MaxLimit)
  }

  private val dateFrom = parameter("date_from".as[DateTime].?)

  private val dateTo = parameter("date_to".as[DateTime].?)

  val paged = (offset & limit).as(Pager) & dateFrom & dateTo

  def respondWithPaginationHeaders(pager: Pager, pagRes: PaginatedResult[_]): Directive0 =
    extract(identity) flatMap { ctx =>
      val baseHeaders = List(
        RawHeader("X-Limit", pager.limit.toString),
        RawHeader("X-Total-Count", pagRes.total.toString)
      )

      respondWithHeaders {
        if (pager.nextOffset >= pagRes.total) baseHeaders
        else {
          val linkParams = Query(ctx.request.uri.query().toMap ++
            Map(
              "offset" -> pager.nextOffset.toString,
              "limit" -> pager.limit.toString
            ))

          baseHeaders :+
            Link(ctx.request.uri.withQuery(linkParams), LinkParams.next)
        }
      }
    }

}
