package app.revanced.arsc.logging
interface Logger {
    fun error(msg: String)
    fun warn(msg: String)
    fun info(msg: String)
    fun trace(msg: String)
}