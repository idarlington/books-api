package com.idarlington.books.resources

import com.twitter.concurrent.AsyncMeter
import com.twitter.finagle.util.DefaultTimer.Implicit
import com.twitter.util.Duration

import java.util.concurrent.TimeUnit

case class Meters(minute: AsyncMeter, daily: AsyncMeter)

object Meters {
  lazy val minuteMeter: AsyncMeter = AsyncMeter.newMeter(5, Duration(1, TimeUnit.MINUTES), 5)
  lazy val dailyMeter: AsyncMeter  = AsyncMeter.newMeter(500, Duration(1, TimeUnit.DAYS), 5)

  def all: Meters = Meters(minute = minuteMeter, daily = dailyMeter)
}
