package app.revanced.patcher.logging

interface Logger {
    fun error(msg: String) {}
    fun warn(msg: String) {}
    fun info(msg: String) {}
    fun trace(msg: String) {}

    object Nop : Logger
}

/**
 * Turn a Patcher [Logger] into an [app.revanced.arsc.logging.Logger].
 */
internal fun Logger.asArscLogger() = object : app.revanced.arsc.logging.Logger {
    override fun error(msg: String) = this@asArscLogger.error(msg)
    override fun warn(msg: String) = this@asArscLogger.warn(msg)
    override fun info(msg: String) = this@asArscLogger.info(msg)
    override fun trace(msg: String) = this@asArscLogger.error(msg)
}