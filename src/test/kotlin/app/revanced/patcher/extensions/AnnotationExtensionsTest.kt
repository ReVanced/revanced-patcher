package app.revanced.patcher.extensions

import app.revanced.patcher.extensions.AnnotationExtensions.findAnnotationRecursively
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private object AnnotationExtensionsTest {
    @Test
    fun `find annotation in annotated class`() {
        assertNotNull(TestClasses.Annotation2::class.findAnnotationRecursively(TestClasses.Annotation::class))
    }

    @Test
    fun `find annotation`() {
        assertNotNull(TestClasses.AnnotatedClass::class.findAnnotationRecursively(TestClasses.Annotation::class))
    }

    @Test
    fun `find annotation recursively in super class`() {
        assertNotNull(TestClasses.AnnotatedClass2::class.findAnnotationRecursively(TestClasses.Annotation::class))
    }

    @Test
    fun `find annotation recursively in super class with annotation`() {
        assertNotNull(TestClasses.AnnotatedTestClass3::class.findAnnotationRecursively(TestClasses.Annotation::class))
    }

    @Test
    fun `don't find unknown annotation in annotated class`() {
        assertNull(TestClasses.AnnotatedClass::class.findAnnotationRecursively(TestClasses.UnknownAnnotation::class))
    }

    object TestClasses {
        annotation class Annotation

        @Annotation
        annotation class Annotation2

        annotation class UnknownAnnotation

        @Annotation
        abstract class AnnotatedClass

        @Annotation2
        class AnnotatedTestClass3

        abstract class AnnotatedClass2 : AnnotatedClass()
    }
}
