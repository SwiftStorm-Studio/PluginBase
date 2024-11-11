package net.rk4z.s1.swiftbase.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.file.Files

@Suppress("unused")
class Core internal constructor(
    val packageName: String,
    val dataFolder: File,
    val version: String,
    val modrinthID: String? = null,
    val useConfigFile: Boolean = true,
    val useLanguageSystem: Boolean = true,
    val isDebug: Boolean = false,
    val availableLang: List<String>? = null,
    val executor: S0Executor,
    /**
     * The root directory of the config files in the JAR.
     * This directory should contain the default config file for the plugin.
     * The default config file should be named as the default language code (e.g. `en.yml`).
     */
    val configFileRoot: String,
    /**
     * The root directory of the language files in the JAR.
     * This directory should contain the default language files for the plugin.
     * The default language files should be named as the language code (e.g. `en.yml`).
     */
    val langDirRoot: String
) {
    companion object {
        lateinit var instance: Core
            private set
        lateinit var languageManager: LanguageManager<*, *>

        var logger: Logger = LoggerFactory.getLogger("SwiftBase")

        fun isInitialized(): Boolean {
            return ::instance.isInitialized
        }
    }

    val helper = ResourceHelper(dataFolder)
    val yaml = Yaml()
    val configFile = dataFolder.resolve("config.yml")
    val langDir = dataFolder.resolve("lang")

    fun String.toBooleanOrNull(): Boolean? {
        return when (this.trim().lowercase()) {
            "true", "1", "t" -> true
            "false", "0", "f" -> false
            else -> null
        }
    }

    inline fun <reified T> lc(key: String): T? {
        val config: Map<String, Any> = Files.newInputStream(configFile.toPath()).use { yaml.load(it) }
        val value = config[key]
        return parseValue(value)
    }

    inline fun <reified T> parseValue(value: Any?): T? {
        return when (T::class) {
            String::class -> value as? T
            Int::class -> value?.toString()?.toIntOrNull() as? T
            Boolean::class -> value?.toString()?.toBooleanOrNull() as? T
            Double::class -> value?.toString()?.toDoubleOrNull() as? T
            Short::class -> value?.toString()?.toShortOrNull() as? T
            Long::class -> value?.toString()?.toLongOrNull() as? T
            Float::class -> value?.toString()?.toFloatOrNull() as? T
            Byte::class -> value?.toString()?.toByteOrNull() as? T
            Char::class -> (value as? String)?.singleOrNull() as? T
            List::class -> value as? List<*> as? T
            Array::class -> (value as? List<*>)?.toTypedArray() as? T
            Map::class -> value as? Map<*, *> as? T
            BigInteger::class -> (value?.toString())?.let { BigInteger(it) } as? T
            BigDecimal::class -> (value?.toString())?.let { BigDecimal(it) } as? T
            else -> value as? T
        }
    }
}