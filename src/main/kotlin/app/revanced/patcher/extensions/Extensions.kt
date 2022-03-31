package app.revanced.patcher.extensions

import org.jf.dexlib2.AccessFlags

infix fun AccessFlags.or(other: AccessFlags) = this.value or other.value