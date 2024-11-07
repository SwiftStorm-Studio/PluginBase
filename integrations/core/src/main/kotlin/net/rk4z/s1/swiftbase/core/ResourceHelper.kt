package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.Core.Companion.logger
import org.jetbrains.annotations.NotNull
import java.io.*

class ResourceHelper internal constructor(
    private val dataFolder: File
) {
    fun saveResource(@NotNull resourcePath: String, replace: Boolean) {
        if (resourcePath.isEmpty()) {
            throw IllegalArgumentException("ResourcePath cannot be null or empty")
        }

        val sanitizedPath = resourcePath.replace('\\', '/')
        val inputStream = getResource(sanitizedPath)
            ?: throw IllegalArgumentException("The embedded resource '$sanitizedPath' cannot be found in the plugin JAR.")

        val outFile = File(dataFolder, sanitizedPath)
        val lastIndex = sanitizedPath.lastIndexOf('/')
        val outDir = File(dataFolder, sanitizedPath.substring(0, if (lastIndex >= 0) lastIndex else 0))

        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        try {
            if (!outFile.exists() || replace) {
                FileOutputStream(outFile).use { out ->
                    inputStream.use { input ->
                        input.copyTo(out)
                    }
                }
                logger.info("Saved resource $sanitizedPath to $outFile")
            } else {
                logger.warn("Could not save $outFile because it already exists.")
            }
        } catch (ex: IOException) {
            logger.warn("Could not save $outFile: ${ex.message}")
        }
    }

    fun getResource(filename: String): InputStream? {
        if (filename.isEmpty()) {
            throw IllegalArgumentException("Filename cannot be null or empty")
        }

        return try {
            val url = this::class.java.classLoader.getResource(filename) ?: return null
            url.openConnection().apply { useCaches = false }.getInputStream()
        } catch (ex: IOException) {
            null
        }
    }
}