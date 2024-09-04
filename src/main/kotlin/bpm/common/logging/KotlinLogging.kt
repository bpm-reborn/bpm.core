package bpm.common.logging

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object KotlinLogging {

    inline fun logger(execute: Logs.() -> Unit): Logs {
        val logger = LogManager.getLogger(Thread.currentThread().stackTrace[2].className)
        val logs = Logs(logger)
        logs.execute()
        return logs
    }
}


class Logs(private val delegate: Logger) {

    fun info(message: String) {
        delegate.info(message)
    }

    inline fun info(execute: () -> String) {
        val string = execute()
        info(string)
    }

    fun error(message: String) {
        delegate.error(message)
    }

    fun error(throwable: Throwable, message: () -> String) {
        val string = message()
        delegate.error(string, throwable)
    }

    inline fun error(execute: () -> String) {
        val string = execute()
        error(string)
    }

    fun debug(message: String) {
        delegate.debug(message)
    }

    inline fun debug(execute: () -> String) {
        val string = execute()
        debug(string)
    }

    fun trace(message: String) {
        delegate.trace(message)
    }

    inline fun trace(execute: () -> String) {
        val string = execute()
        trace(string)
    }

    fun warn(message: String) {
        delegate.warn(message)
    }

    inline fun warn(execute: () -> String) {
        val string = execute()
        warn(string)
    }

    fun fatal(message: String) {
        delegate.fatal(message)
    }

    inline fun fatal(execute: () -> String) {
        val string = execute()
        fatal(string)
    }


}

