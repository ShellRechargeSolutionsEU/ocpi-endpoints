package com.thenewmotion.ocpi
package common

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.{Link, LinkParams, RawHeader}
import akka.http.scaladsl.server.{Directive0, Directives}

case class PaginatedResult[+T](result: Iterable[T], total: Int)

case class Pager(offset: Int, limit: Int)

trait PaginatedRoute extends Directives {

  val DefaultOffset = 0
  def DefaultLimit: Int

  val paged =
    parameters(('offset.as[Int] ? DefaultOffset, 'limit.as[Int] ? DefaultLimit))

  def respondWithPaginationHeaders(offset: Int, limitToUse: Int,
                                   otherLinkParameters: Map[String, String], pagRes: PaginatedResult[_]): Directive0 =
    extract(identity) flatMap { ctx =>
      respondWithHeaders(
        if (offset + limitToUse >= pagRes.total) Nil
        else {
          val linkParams = Query(otherLinkParameters ++
            Map(
              "offset" -> (offset + limitToUse).toString,
              "limit" -> limitToUse.toString
            ))

          List(
            Link(ctx.request.uri.withQuery(linkParams), LinkParams.next),
            RawHeader("X-Limit", limitToUse.toString),
            RawHeader("X-Total-Count", pagRes.total.toString)
          )
        }
      )
    }
}
