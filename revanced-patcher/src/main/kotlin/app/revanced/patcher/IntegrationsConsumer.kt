package app.revanced.patcher

import java.io.File

@FunctionalInterface
interface IntegrationsConsumer {
    fun acceptIntegrations(integrations: List<File>)
}