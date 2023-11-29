@file:Suppress("UNCHECKED_CAST")

package app.revanced.patcher.extensions

import kotlin.reflect.KClass

internal object AnnotationExtensions {
    /**
     * Search for an annotation recursively.
     *
     * @param targetAnnotationClass The annotation class to search for.
     * @param searchedClasses A set of annotations that have already been searched.
     * @return The annotation if found, otherwise null.
     */
    fun <T : Annotation> Class<*>.findAnnotationRecursively(
        targetAnnotationClass: Class<T>,
        searchedClasses: HashSet<Annotation> = hashSetOf(),
    ): T? {
        annotations.forEach { annotation ->
            // Terminate if the annotation is already searched.
            if (annotation in searchedClasses) return@forEach
            searchedClasses.add(annotation)

            // Terminate if the annotation is found.
            if (targetAnnotationClass == annotation.annotationClass.java) return annotation as T

            return annotation.annotationClass.java.findAnnotationRecursively(
                targetAnnotationClass,
                searchedClasses,
            ) ?: return@forEach
        }

        // Search the super class.
        superclass?.findAnnotationRecursively(
            targetAnnotationClass,
            searchedClasses,
        )?.let { return it }

        // Search the interfaces.
        interfaces.forEach { superClass ->
            return superClass.findAnnotationRecursively(
                targetAnnotationClass,
                searchedClasses,
            ) ?: return@forEach
        }

        return null
    }

    /**
     * Search for an annotation recursively.
     *
     * First the annotations, then the annotated classes super class and then it's interfaces
     * are searched for the annotation recursively.
     *
     * @param targetAnnotation The annotation to search for.
     * @return The annotation if found, otherwise null.
     */
    fun <T : Annotation> KClass<*>.findAnnotationRecursively(targetAnnotation: KClass<T>) =
        java.findAnnotationRecursively(targetAnnotation.java)
}
