package com.thenewmotion.ocpi

import spray.http.StatusCodes._
import spray.http.HttpResponse
import spray.routing._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * This is not OCPI but a TNM extensions for monitoring purposes,
 * which has been put here with the other route definitions for now
 * so it can be easily composed with the OCPI routes.
 * It might be moved out of this library again in the future.
 */
trait StatusRoute extends HttpService {

  import scala.concurrent.ExecutionContext.Implicits.global

  def statusRoute(checks: List[StatusCheck]): Route = get {
    (path("status") & pathEnd) {
      complete {
        val res: HttpResponse = Await.result(StatusService(checks), 3.seconds)
        res
      }
    }
  }
}

/**
 * A status check is a future that succeeds if the check is OK, and fails with an informative exception if there is
 * a problem. The status page displays the result of all status checks combined.
 */
case class StatusCheck(serviceName: String, checkOperation: Future[Unit])(implicit ec: ExecutionContext) {
  def check: Future[Option[String]] = checkOperation map { _ => None } recover {
    case ex: Exception => Some(s"Error with service $serviceName: ${ex.getMessage}\n${ex.getStackTrace}")
  }
}

object StatusService {
  val logger = Logger(getClass)

  def apply(checks: List[StatusCheck])(implicit ec: ExecutionContext): Future[HttpResponse] = {

    logger.debug(s"Checks enabled: ${checks.map(_.serviceName).mkString(",")}")

    Future.traverse(checks)(_.check).map {
      _.flatten match {
        case Nil => HttpResponse(OK, "OK")
        case list => HttpResponse(InternalServerError, list.mkString("Errors: \n\n", "\n\n", ""))
      }
    }
  }
}