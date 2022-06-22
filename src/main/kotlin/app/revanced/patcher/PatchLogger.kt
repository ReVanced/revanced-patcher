package app.revanced.patcher

interface PatchLogger {
    fun error(msg: String)
    fun warn(msg: String)
    fun info(msg: String)
    fun trace(msg: String)
}

internal object NoopPatchLogger : PatchLogger {
    override fun error(msg: String) {}
    override fun warn(msg: String) {}
    override fun info(msg: String) {}
    override fun trace(msg: String) {}
}