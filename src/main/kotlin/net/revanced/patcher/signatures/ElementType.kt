package net.revanced.patcher.signatures

class ElementType private constructor() {
    companion object {
        const val Void = "()V"
        const val Boolean = "()Z"
        const val Byte = "()B"
        const val Char = "()C"
        const val Short = "()S"
        const val Int = "()I"
        const val Long = "()J"
        const val Float = "()F"
        const val Double = "()D"
    }
}