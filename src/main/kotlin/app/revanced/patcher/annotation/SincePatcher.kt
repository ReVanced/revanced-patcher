package app.revanced.patcher.annotation

import app.revanced.patcher.patch.Patch
import app.revanced.patcher.Patcher

/**
 * Declares a [Patch] deprecated for removal.
 * @param version The minimum version of the [Patcher] this [Patch] supports.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SincePatcher(val version: String)
