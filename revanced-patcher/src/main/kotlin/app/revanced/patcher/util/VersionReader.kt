package app.revanced.patcher.util

import java.util.*

@Deprecated("This class serves no purpose anymore")
internal object VersionReader {
    @JvmStatic
    private val properties = Properties().apply {
        load(
            VersionReader::class.java.getResourceAsStream("/app/revanced/patcher/version.properties")
                ?: throw IllegalStateException("Could not load version.properties")
        )
    }

    @JvmStatic
    fun read(): String {
        return properties.getProperty("version") ?: throw IllegalStateException("Version not found")
    }
}