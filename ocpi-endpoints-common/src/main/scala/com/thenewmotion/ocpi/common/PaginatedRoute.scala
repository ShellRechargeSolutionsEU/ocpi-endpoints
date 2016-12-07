package com.thenewmotion.ocpi.common

import spray.http.HttpHeaders.{Link, RawHeader}
import spray.routing.{Directive0, Directives}

case class PaginatedResult[+T](result: Iterable[T], total: Int)

case class Pager(offset: Int, limit: Int)

trait PaginatedRoute extends Directives{

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
          val linkParams = otherLinkParameters ++
            Map(
              "offset" -> (offset + limitToUse).toString,
              "limit" -> limitToUse.toString
            )

          List(
            Link(Link.Value(ctx.request.uri.withQuery(linkParams), Link.next)),
            RawHeader("X-Limit", limitToUse.toString),
            RawHeader("X-Total-Count", pagRes.total.toString)
          )
        }



      )
    }
}
