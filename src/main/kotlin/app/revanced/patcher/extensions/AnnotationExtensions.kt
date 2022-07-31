package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.Data
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.annotations.Dependencies
import app.revanced.patcher.patch.annotations.DependencyType
import app.revanced.patcher.patch.annotations.DependsOn
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

private typealias PatchClass = Class<out Patch<Data>>

object PatchExtensions {
    val PatchClass.patchName: String get() = recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val PatchClass.version get() = recursiveAnnotation(Version::class)?.version
    val PatchClass.include get() = recursiveAnnotation(app.revanced.patcher.patch.annotations.Patch::class)!!.include
    val PatchClass.description get() = recursiveAnnotation(Description::class)?.description
    val PatchClass.dependencies get() = buildList {
        recursiveAnnotation(DependsOn::class)?.let { add(PatchDependency(it.value, it.type)) }
        recursiveAnnotation(Dependencies::class)?.dependencies?.forEach { add(PatchDependency(it, DependencyType.HARD)) }
    }.toTypedArray()
    val PatchClass.compatiblePackages get() = recursiveAnnotation(Compatibility::class)?.compatiblePackages

    @JvmStatic
    fun PatchClass.dependsOn(patch: PatchClass): Boolean {
        if (this.patchName == patch.patchName) throw IllegalArgumentException("thisval and patch may not be the same")
        return this.dependencies.any { it.patch.java.patchName == this@dependsOn.patchName }
    }

    class PatchDependency internal constructor(val patch: KClass<out Patch<Data>>, val type: DependencyType = DependencyType.HARD)
}

object MethodFingerprintExtensions {
    val MethodFingerprint.name: String
        get() = javaClass.recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val MethodFingerprint.version get() = javaClass.recursiveAnnotation(Version::class)?.version ?: "0.0.1"
    val MethodFingerprint.description get() = javaClass.recursiveAnnotation(Description::class)?.description
    val MethodFingerprint.compatiblePackages get() = javaClass.recursiveAnnotation(Compatibility::class)?.compatiblePackages
    val MethodFingerprint.matchingMethod get() = javaClass.recursiveAnnotation(app.revanced.patcher.fingerprint.method.annotation.MatchingMethod::class)
    val MethodFingerprint.fuzzyPatternScanMethod get() = javaClass.recursiveAnnotation(app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod::class)
    val MethodFingerprint.fuzzyScanThreshold get() = fuzzyPatternScanMethod?.threshold ?: 0
}