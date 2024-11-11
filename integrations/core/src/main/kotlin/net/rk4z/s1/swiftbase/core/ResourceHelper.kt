package net.rk4z.s1.swiftbase.core

import net.rk4z.s1.swiftbase.core.Core.Companion.logger
import org.jetbrains.annotations.NotNull
import java.io.*

/**
 * Utility class for handling resources embedded within the plugin JAR. This class provides
 * methods to save resources from the JAR to a specified data folder and retrieve resources
 * as input streams.
 *
 * @property dataFolder The root directory where resources will be saved.
 */
class ResourceHelper internal constructor(
    private val dataFolder: File
) {

    /**
     * Saves a specified resource from the JAR to the `dataFolder` or a specified output directory.
     * Creates any necessary directories if they do not already exist.
     *
     * @param resourcePath The path of the resource within the JAR file.
     * @param replace Specifies whether to replace the file if it already exists.
     * @param outPath Optional output path where the file should be saved. If null, defaults to `dataFolder`.
     * @throws IllegalArgumentException If the resource path is empty or the resource is not found in the JAR.
     */
    fun saveResource(@NotNull resourcePath: String, replace: Boolean, outPath: String? = null) {
        if (resourcePath.isEmpty()) {
            throw IllegalArgumentException("ResourcePath cannot be null or empty")
        }

        logger.info("Finding resource $resourcePath")

        val sanitizedPath = resourcePath.replace('\\', '/')
        val inputStream = getResource(sanitizedPath)
            ?: throw IllegalArgumentException("The embedded resource '$sanitizedPath' cannot be found in the plugin JAR.")

        val outputFolder = outPath?.let { File(it) } ?: dataFolder
        val outFile = if (outPath != null) {
            File(outputFolder, File(sanitizedPath).name)
        } else {
            File(outputFolder, sanitizedPath)
        }

        val outDir = outFile.parentFile
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


    /**
     * Retrieves an embedded resource from the JAR as an [InputStream].
     *
     * @param filename The name of the resource file within the JAR.
     * @return An [InputStream] for the resource, or `null` if the resource is not found.
     * @throws IllegalArgumentException If the filename is empty.
     */
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
