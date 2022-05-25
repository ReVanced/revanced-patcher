package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.signature.implementation.method.MethodSignature
import app.revanced.patcher.signature.implementation.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.signature.implementation.method.annotation.MatchingMethod

private inline fun <reified T : Annotation> Any.firstAnnotation() =
    this::class.annotations.first { it is T } as T

private inline fun <reified T : Annotation> Any.recursiveAnnotation() =
    this::class.java.findAnnotationRecursively(T::class.java)!!

object PatchExtensions {
    val Patch<*>.name get() = firstAnnotation<Name>().name
    val Patch<*>.version get() = firstAnnotation<Version>().version
    val Patch<*>.description get() = firstAnnotation<Description>().description
    val Patch<*>.compatiblePackages get() = recursiveAnnotation<Compatibility>().compatiblePackages
}

object MethodSignatureExtensions {
    val MethodSignature.name get() = firstAnnotation<Name>().name
    val MethodSignature.version get() = firstAnnotation<Version>().version
    val MethodSignature.description get() = firstAnnotation<Description>().description
    val MethodSignature.compatiblePackages get() = recursiveAnnotation<Compatibility>().compatiblePackages
    val MethodSignature.matchingMethod get() = firstAnnotation<MatchingMethod>()
    val MethodSignature.fuzzyThreshold get() = firstAnnotation<FuzzyPatternScanMethod>().threshold
}