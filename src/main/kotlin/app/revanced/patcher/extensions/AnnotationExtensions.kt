package app.revanced.patcher.extensions

import app.revanced.patcher.annotation.*
import app.revanced.patcher.data.Context
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchOptions
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.RequiresIntegrations
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

/**
 * Recursively find a given annotation on a class.
 * @param targetAnnotation The annotation to find.
 * @return The annotation.
 */
private fun <T : Annotation> Class<*>.findAnnotationRecursively(targetAnnotation: KClass<T>) =
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
    val Class<out Patch<Context>>.patchName: String
        get() = findAnnotationRecursively(Name::class)?.name ?: this.simpleName

    val Class<out Patch<Context>>.version
        get() = findAnnotationRecursively(Version::class)?.version

    val Class<out Patch<Context>>.include
        get() = findAnnotationRecursively(app.revanced.patcher.patch.annotations.Patch::class)!!.include

    val Class<out Patch<Context>>.description
        get() = findAnnotationRecursively(Description::class)?.description

    val Class<out Patch<Context>>.dependencies
        get() = findAnnotationRecursively(DependsOn::class)?.dependencies

    val Class<out Patch<Context>>.compatiblePackages
        get() = findAnnotationRecursively(Compatibility::class)?.compatiblePackages

    internal val Class<out Patch<Context>>.requiresIntegrations
        get() = findAnnotationRecursively(RequiresIntegrations::class) != null

    val Class<out Patch<Context>>.options: PatchOptions?
        get() = kotlin.companionObject?.let { cl ->
            if (cl.visibility != KVisibility.PUBLIC) return null
            kotlin.companionObjectInstance?.let {
                (it as? OptionsContainer)?.options
            }
        }
}

object MethodFingerprintExtensions {
    val MethodFingerprint.name: String
        get() = this.javaClass.simpleName

    val MethodFingerprint.fuzzyPatternScanMethod
        get() = javaClass.findAnnotationRecursively(FuzzyPatternScanMethod::class)

    val MethodFingerprint.fuzzyScanThreshold
        get() = fuzzyPatternScanMethod?.threshold ?: 0
}