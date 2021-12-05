package com.app.ticker.util

import org.slf4j.event.Level
import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  protected val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  def logIt[A](text: String, level: Level = Level.INFO)(a: A): A = {
    def logFn = (msg: String) =>
      level match {
        case Level.ERROR => logger.info(msg)
        case Level.WARN  => logger.warn(msg)
        case Level.INFO  => logger.info(msg)
        case Level.DEBUG => logger.debug(msg)
        case Level.TRACE => logger.trace(msg)
      }
    logFn(s"$text : $a")
    a
  }
}
