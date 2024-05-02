package app.revanced.patcher

import java.io.File

fun interface IntegrationsConsumer {
    fun acceptIntegrations(integrations: Set<File>)

    @Deprecated("Use acceptIntegrations(Set<File>) instead.")
    fun acceptIntegrations(integrations: List<File>) { }
}
