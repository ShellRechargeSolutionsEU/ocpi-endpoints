package com.thenewmotion.ocpi.common

import spray.http.HttpHeaders.{Link, RawHeader}
import spray.routing.{Directive0, Directives}

case class PaginatedResult[+T](result: Iterable[T], limit: Int, total: Int)

case class Pager(offset: Int, limit: Int)

trait PaginatedRoute extends Directives{

  val DefaultOffset = 0
  val DefaultLimit = 1000

  val paged =
    parameters(('offset.as[Int] ? DefaultOffset, 'limit.as[Int] ? DefaultLimit))

  def respondWithPaginationHeaders(offset: Int, pagRes: PaginatedResult[_]): Directive0 =
    extract(identity) flatMap { ctx => respondWithHeaders(List(
      Link(Link.Value(ctx.request.uri.withQuery(
        ("offset", (offset + pagRes.limit).toString),
        ("limit", pagRes.limit.toString)), Link.next)),
      RawHeader("X-Limit", pagRes.limit.toString),
      RawHeader("X-Total-Count", pagRes.total.toString)))}
}
