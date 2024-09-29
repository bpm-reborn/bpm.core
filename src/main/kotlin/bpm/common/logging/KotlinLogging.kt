package bpm.common.logging

import org.apache.logging.log4j.LogManager
import java.util.Stack

object KotlinLogging {

    private val cachedLogs = mutableMapOf<StackTraceElement, Logs>()

    inline fun logger(crossinline execute: Logs.() -> Unit): Logs {
        val logs = logger(Thread.currentThread().stackTrace[2])
        //Can set the identity here
        logs.execute()
        return logs
    }

    fun logger(stacktrace: StackTraceElement = Thread.currentThread().stackTrace[2]): Logs {
        return cachedLogs.getOrPut(stacktrace) { Logs(stacktrace) }
    }


    class Logs(stacktrace: StackTraceElement) {

        val className: String = stacktrace.className

        val methodName: String = stacktrace.methodName

        val lineNumber: Int = stacktrace.lineNumber

        val fileName: String = stacktrace.fileName ?: "Unknown"

        var identity = "$className#$methodName - $fileName:$lineNumber"
            set(value) {
                field = value
                delegate = LogManager.getLogger(value)
            }

        private var delegate = LogManager.getLogger(identity)


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

        inline fun warn(crossinline execute: Logs.() -> String) {
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

}

