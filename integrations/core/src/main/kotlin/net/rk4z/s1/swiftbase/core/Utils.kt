package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.dummy.DummyExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

val dummyCore = Core(
    packageName = "com.example.dummy",
    isDebug = true,
    dataFolder = File("dummyData"),
    configFile = null,
    configResourceRoot = "/dummy/config",
    availableLang = listOf("en", "ja"),
    langDir = File("dummyLang"),
    langResourceRoot = "/dummy/lang",
    executor = DummyExecutor(),
    logger = LoggerFactory.getLogger("DummyCore"),
    modrinthID = "dummy",
    version = "1.0.0"
)

fun String.toBooleanOrNull(): Boolean? {
    return when (this.trim().lowercase()) {
        "true", "1", "t" -> true
        "false", "0", "f" -> false
        else -> null
    }
}

fun <T : Any> KClass<T>.createInstanceOrNull(): T? {
    return try {
        this.createInstance()
    } catch (e: Exception) {
        Logger.logIfDebug("Failed to create instance for class: ${this.simpleName}, reason: ${e.message}")
        null
    }
}


fun String.isBlankOrEmpty(): Boolean {
    return this.isBlank() || this.isEmpty()
}

fun File.notExists(): Boolean {
    return !this.exists()
}

fun Logger.logIfDebug(message: String, level: LogLevel = LogLevel.INFO) {
    if (Core.getInstance().isDebug) {
        when (level) {
            LogLevel.INFO -> info(message)
            LogLevel.WARN -> warn(message)
            LogLevel.ERROR -> error(message)
        }
    } else {
        debug(message)
    }
}

enum class LogLevel {
    INFO, WARN, ERROR
}