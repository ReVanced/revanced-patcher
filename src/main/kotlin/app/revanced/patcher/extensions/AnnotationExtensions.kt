package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.*
import app.revanced.patcher.data.Data
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchOptions
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

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
    val Class<*>.patchName: String get() = recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val Class<out Patch<Data>>.version get() = recursiveAnnotation(Version::class)?.version
    val Class<out Patch<Data>>.include get() = recursiveAnnotation(app.revanced.patcher.patch.annotations.Patch::class)!!.include
    val Class<out Patch<Data>>.description get() = recursiveAnnotation(Description::class)?.description
    val Class<out Patch<Data>>.dependencies get() = recursiveAnnotation(app.revanced.patcher.patch.annotations.DependsOn::class)?.dependencies
    val Class<out Patch<Data>>.compatiblePackages get() = recursiveAnnotation(Compatibility::class)?.compatiblePackages
    val Class<out Patch<Data>>.options: PatchOptions?
        get() = kotlin.companionObject?.let { cl ->
            if (cl.visibility != KVisibility.PUBLIC) return null
            kotlin.companionObjectInstance?.let {
                (it as? OptionsContainer)?.options
            }
        }
    val Class<out Patch<Data>>.deprecated: Pair<String, KClass<out Patch<Data>>?>?
        get() = recursiveAnnotation(PatchDeprecated::class)?.let {
            it.reason to it.replacement.let { cl ->
                if (cl == Patch::class) null else cl
            }
        }
}

object MethodFingerprintExtensions {
    val MethodFingerprint.name: String
        get() = javaClass.recursiveAnnotation(Name::class)?.name ?: this.javaClass.simpleName
    val MethodFingerprint.version get() = javaClass.recursiveAnnotation(Version::class)?.version ?: "0.0.1"
    val MethodFingerprint.description get() = javaClass.recursiveAnnotation(Description::class)?.description
    val MethodFingerprint.fuzzyPatternScanMethod get() = javaClass.recursiveAnnotation(app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod::class)
    val MethodFingerprint.fuzzyScanThreshold get() = fuzzyPatternScanMethod?.threshold ?: 0
}