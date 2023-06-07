package app.revanced.patcher.extensions

import kotlin.reflect.KClass

internal object AnnotationExtensions {
    /**
     * Recursively find a given annotation on a class.
     *
     * @param targetAnnotation The annotation to find.
     * @return The annotation.
     */
    fun <T : Annotation> Class<*>.findAnnotationRecursively(targetAnnotation: KClass<T>): T? {
        fun <T : Annotation> Class<*>.findAnnotationRecursively(
            targetAnnotation: Class<T>, traversed: MutableSet<Annotation>
        ): T? {
            val found = this.annotations.firstOrNull { it.annotationClass.java.name == targetAnnotation.name }

            @Suppress("UNCHECKED_CAST") if (found != null) return found as T

            for (annotation in this.annotations) {
                if (traversed.contains(annotation)) continue
                traversed.add(annotation)

                return (annotation.annotationClass.java.findAnnotationRecursively(targetAnnotation, traversed))
                    ?: continue
            }

            return null
        }

        return this.findAnnotationRecursively(targetAnnotation.java, mutableSetOf())
    }
}