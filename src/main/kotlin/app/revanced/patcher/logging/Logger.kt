package app.revanced.patcher.logging

@Deprecated("This will be removed in a future release")
interface Logger {
    fun error(msg: String) {}
    fun warn(msg: String) {}
    fun info(msg: String) {}
    fun trace(msg: String) {}
}