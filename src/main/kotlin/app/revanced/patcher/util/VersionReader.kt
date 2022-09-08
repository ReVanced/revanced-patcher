package app.revanced.patcher.util

import java.util.*

internal object VersionReader {
    @JvmStatic
    private val props = Properties().apply {
        load(
            VersionReader::class.java.getResourceAsStream("/revanced-patcher/version.properties")
                ?: throw IllegalStateException("Could not load version.properties")
        )
    }

    @JvmStatic
    fun read(): String {
        return props.getProperty("version") ?: throw IllegalStateException("Version not found")
    }
}