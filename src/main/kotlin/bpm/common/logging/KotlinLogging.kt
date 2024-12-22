package bpm.common.logging

import org.apache.logging.log4j.LogManager
import java.util.Stack

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class CallsiteAware

object KotlinLogging {

    private val cachedLogs = mutableMapOf<String, Logs>()

    @JvmStatic
    @CallsiteAware
    fun logger(func: Logs.() -> Unit): Logs {
        val element = Thread.currentThread().stackTrace[2]
        val name = getLoggerName(element)
        val logger = cachedLogs.getOrPut(name) { Logs(name) }
        logger.func()
        return logger
    }

    @JvmStatic
    @CallsiteAware
    fun logger(name: String, func: Logs.() -> Unit): Logs {
        val logger = cachedLogs.getOrPut(name) { Logs(name) }
        logger.func()
        return logger
    }

    private fun getLoggerName(element: StackTraceElement): String {
        val className = element.className
        return (when {
            className.contains("$") -> {
                element.fileName?.removeSuffix(".kt") ?: "Unknown"
            }

            else -> className
        }.substringAfterLast(".").removeSuffix("Kt"))
    }

    class Logs(name: String) {

        var identity = name
            set(value) {
                field = value
                delegate = LogManager.getLogger(value)
            }

        private var delegate = LogManager.getLogger(identity)

        fun info(message: String) = delegate.info(message)
        inline fun info(execute: () -> String) = info(execute())

        fun error(message: String) = delegate.error(message)
        fun error(throwable: Throwable, message: () -> String) = delegate.error(message(), throwable)
        inline fun error(execute: () -> String) = error(execute())

        fun debug(message: String) = delegate.debug(message)
        inline fun debug(execute: () -> String) = debug(execute())

        fun trace(message: String) = delegate.trace(message)
        inline fun trace(execute: () -> String) = trace(execute())

        fun warn(message: String) = delegate.warn(message)
        inline fun warn(crossinline execute: Logs.() -> String) = warn(execute())

        fun fatal(message: String) = delegate.fatal(message)
        inline fun fatal(execute: () -> String) = fatal(execute())
    }
}