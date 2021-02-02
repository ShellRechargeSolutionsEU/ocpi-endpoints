package com.thenewmotion.ocpi.common

import org.specs2.matcher.{Expectable, IOMatchers, MatchFailure, MatchResult, Matcher, ValueChecksBase}

trait IOMatchersExt extends IOMatchers with ValueChecksBase {

  def returnValueLike[T](pattern: PartialFunction[T, MatchResult[_]]): IOMatcher[T] = {
    val m: Matcher[T] = new Matcher[T] {
      override def apply[S <: T](a: Expectable[S]) = {
        val r = if (pattern.isDefinedAt(a.value)) pattern.apply(a.value) else MatchFailure("", "", a)
        result(r.isSuccess,
          a.description + " is correct: " + r.message,
          a.description + " is incorrect: " + r.message,
          a)
      }
    }
    attemptRun(m, None)
  }
}
