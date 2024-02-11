package app.revanced.patcher

import java.io.File

@FunctionalInterface
interface IntegrationsConsumer {
    fun acceptIntegrations(integrations: Set<File>)

    @Deprecated("Use acceptIntegrations(Set<File>) instead.")
    fun acceptIntegrations(integrations: List<File>)
}
