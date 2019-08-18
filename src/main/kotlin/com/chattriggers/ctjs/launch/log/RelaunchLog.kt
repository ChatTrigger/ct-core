package com.chattriggers.ctjs.launch.log

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

object RelaunchLog {
    private val logger = LogManager.getLogger("CT")

    fun log(targetLog: String, level: Level, format: String, vararg data: Any?) {
        LogManager.getLogger(targetLog).log(level, String.format(format, *data))
    }

    fun log(level: Level, format: String, vararg data: Any?) {
        logger.log(level, String.format(format, *data))
    }

    fun log(targetLog: String, level: Level, ex: Throwable, format: String, vararg data: Any?) {
        LogManager.getLogger(targetLog).log(level, String.format(format, *data), ex)
    }

    fun log(level: Level, ex: Throwable, format: String, vararg data: Any?) {
        logger.log(level, String.format(format, *data), ex)
    }

    fun severe(format: String, vararg data: Any?) {
        log(Level.ERROR, format, *data)
    }

    fun warning(format: String, vararg data: Any?) {
        log(Level.WARN, format, *data)
    }

    fun info(format: String, vararg data: Any?) {
        log(Level.INFO, format, *data)
    }

    fun fine(format: String, vararg data: Any?) {
        log(Level.DEBUG, format, *data)
    }

    fun finer(format: String, vararg data: Any?) {
        log(Level.TRACE, format, *data)
    }
}