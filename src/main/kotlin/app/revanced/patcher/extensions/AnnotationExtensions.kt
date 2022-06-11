package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.base.Data
import app.revanced.patcher.patch.base.Patch
import app.revanced.patcher.signature.implementation.method.MethodSignature
import app.revanced.patcher.signature.implementation.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.signature.implementation.method.annotation.MatchingMethod
import kotlin.reflect.KClass

/**
 * Recursively find a given annotation on a class.
 * @param targetAnnotation The annotation to find.
 * @return The annotation.
 */
private fun <T : Annotation> Class<*>.recursiveAnnotation(targetAnnotation: KClass<T>) =
    this.findAnnotationRecursively(targetAnnotation.java, mutableSetOf())


private fun <T : Annotation> Class<*>.findAnnotationRecursively(
    targetAnnotation: Class<T>, traversed: MutableSet<Annotation>
): T? {
    val found = this.annotations.firstOrNull { it.annotationClass.java.name == targetAnnotation.name }

    @Suppress("UNCHECKED_CAST") if (found != null) return found as T

    for (annotation in this.annotations) {
        if (traversed.contains(annotation)) continue
        traversed.add(annotation)

        return (annotation.annotationClass.java.findAnnotationRecursively(targetAnnotation, traversed)) ?: continue
    }

    return null
}

object PatchExtensions {
    val Class<out Patch<Data>>.patchName: String
        get() = recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val Class<out Patch<Data>>.version get() = recursiveAnnotation(Version::class)?.version
    val Class<out Patch<Data>>.include get() = recursiveAnnotation(app.revanced.patcher.patch.annotations.Patch::class)!!.include
    val Class<out Patch<Data>>.description get() = recursiveAnnotation(Description::class)?.description
    val Class<out Patch<Data>>.dependencies get() = recursiveAnnotation(app.revanced.patcher.patch.annotations.Dependencies::class)?.dependencies
    val Class<out Patch<Data>>.compatiblePackages get() = recursiveAnnotation(Compatibility::class)?.compatiblePackages
}

object MethodSignatureExtensions {
    val MethodSignature.name: String
        get() = javaClass.recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val MethodSignature.version get() = javaClass.recursiveAnnotation(Version::class)?.version ?: "0.0.1"
    val MethodSignature.description get() = javaClass.recursiveAnnotation(Description::class)?.description
    val MethodSignature.compatiblePackages get() = javaClass.recursiveAnnotation(Compatibility::class)?.compatiblePackages
    val MethodSignature.matchingMethod get() = javaClass.recursiveAnnotation(MatchingMethod::class)
    val MethodSignature.fuzzyPatternScanMethod get() = javaClass.recursiveAnnotation(FuzzyPatternScanMethod::class)
    val MethodSignature.fuzzyThreshold get() = fuzzyPatternScanMethod?.threshold ?: 0
}