package app.revanced.patcher.extensions

internal object AnnotationExtensions {
    /**
     * Search for an annotation recursively.
     *
     * @param targetAnnotation The annotation to search for.
     * @return The annotation if found, otherwise null.
     */
    fun <T : Annotation> Class<*>.findAnnotationRecursively(targetAnnotation: Class<T>): T? {
        fun <T : Annotation> Class<*>.findAnnotationRecursively(
            targetAnnotation: Class<T>,
            searchedAnnotations: MutableSet<Annotation>,
        ): T? {
            val found = this.annotations.firstOrNull { it.annotationClass.java.name == targetAnnotation.name }

            @Suppress("UNCHECKED_CAST")
            if (found != null) return found as T

            for (annotation in this.annotations) {
                if (searchedAnnotations.contains(annotation)) continue
                searchedAnnotations.add(annotation)

                return annotation.annotationClass.java.findAnnotationRecursively(
                    targetAnnotation,
                    searchedAnnotations
                ) ?: continue
            }

            return null
        }

        return this.findAnnotationRecursively(targetAnnotation, mutableSetOf())
    }
}
