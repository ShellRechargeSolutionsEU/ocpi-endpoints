package com.thenewmotion.ocpi.common

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.headers.{Authorization, RawHeader}

object HttpLogging {

  private val redactedHeaders: Map[String, String => String] = Map(
    Authorization.name -> (s => s.take("Token 123".length) + "**REDACTED**")
  )

  def redactHttpRequest(httpReq: HttpRequest): String =
    httpReq.mapHeaders{ headers => headers.map { h =>
      RawHeader(h.name(), redactedHeaders.getOrElse(h.name, identity[String](_))(h.value))
    }
  }.toString

  def redactHttpResponse(httpRes: HttpResponse): String =
    httpRes.mapHeaders{ headers => headers.map { h =>
      RawHeader(h.name(), redactedHeaders.getOrElse(h.name, identity[String](_))(h.value))
    }
  }.toString
}
