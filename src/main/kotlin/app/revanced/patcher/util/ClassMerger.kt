package app.revanced.patcher.util

import app.revanced.patcher.BytecodeContext
import app.revanced.patcher.extensions.or
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.util.ClassMerger.Utils.asMutableClass
import app.revanced.patcher.util.ClassMerger.Utils.filterAny
import app.revanced.patcher.util.ClassMerger.Utils.filterNotAny
import app.revanced.patcher.util.ClassMerger.Utils.isPublic
import app.revanced.patcher.util.ClassMerger.Utils.toPublic
import app.revanced.patcher.util.TypeUtil.traverseClassHierarchy
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableField
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.util.MethodUtil
import kotlin.reflect.KFunction2

/**
 * Experimental class to merge a [ClassDef] with another.
 * Note: This will not consider method implementations or if the class is missing a superclass or interfaces.
 */
internal object ClassMerger {
    /**
     * Merge a class with [otherClass].
     *
     * @param otherClass The class to merge with
     * @param context The context to traverse the class hierarchy in.
     * @param logger A logger.
     */
    fun ClassDef.merge(otherClass: ClassDef, context: BytecodeContext, logger: Logger? = null) = this
        //.fixFieldAccess(otherClass, logger)
        //.fixMethodAccess(otherClass, logger)
        .addMissingFields(otherClass, logger)
        .addMissingMethods(otherClass, logger)
        .publicize(otherClass, context, logger)

    /**
     * Add methods which are missing but existing in [fromClass].
     *
     * @param fromClass The class to add missing methods from.
     * @param logger A logger.
     */
    private fun ClassDef.addMissingMethods(fromClass: ClassDef, logger: Logger? = null): ClassDef {
        val missingMethods = fromClass.methods.let { fromMethods ->
            methods.filterNot { method ->
                fromMethods.any { fromMethod ->
                    MethodUtil.methodSignaturesMatch(fromMethod, method)
                }
            }
        }

        if (missingMethods.isEmpty()) return this

        logger?.trace("Found ${missingMethods.size} missing methods")

        return asMutableClass().apply {
            methods.addAll(missingMethods.map { it.toMutable() })
        }
    }

    /**
     * Add fields which are missing but existing in [fromClass].
     *
     * @param fromClass The class to add missing fields from.
     * @param logger A logger.
     */
    private fun ClassDef.addMissingFields(fromClass: ClassDef, logger: Logger? = null): ClassDef {
        val missingFields = fields.filterNotAny(fromClass.fields) { field, fromField ->
            fromField.name == field.name
        }

        if (missingFields.isEmpty()) return this

        logger?.trace("Found ${missingFields.size} missing fields")

        return asMutableClass().apply {
            fields.addAll(missingFields.map { it.toMutable() })
        }
    }

    /**
     * Make a class and its super class public recursively.
     * @param reference The class to check the [AccessFlags] of.
     * @param context The context to traverse the class hierarchy in.
     * @param logger A logger.
     */
    private fun ClassDef.publicize(reference: ClassDef, context: BytecodeContext, logger: Logger? = null) =
        if (reference.accessFlags.isPublic() && !accessFlags.isPublic())
            this.asMutableClass().apply {
                context.traverseClassHierarchy(this) {
                    if (accessFlags.isPublic()) return@traverseClassHierarchy

                    logger?.trace("Publicizing ${this.type}")

                    accessFlags = accessFlags.toPublic()
                }
            }
        else this

    /**
     * Publicize fields if they are public in [reference].
     *
     * @param reference The class to check the [AccessFlags] of the fields in.
     * @param logger A logger.
     */
    private fun ClassDef.fixFieldAccess(reference: ClassDef, logger: Logger? = null): ClassDef {
        val brokenFields = fields.filterAny(reference.fields) { field, referenceField ->
            if (field.name != referenceField.name) return@filterAny false

            referenceField.accessFlags.isPublic() && !field.accessFlags.isPublic()
        }

        if (brokenFields.isEmpty()) return this

        logger?.trace("Found ${brokenFields.size} broken fields")

        /**
         * Make a field public.
         */
        fun MutableField.publicize() {
            accessFlags = accessFlags.toPublic()
        }

        return asMutableClass().apply {
            fields.filter { brokenFields.contains(it) }.forEach(MutableField::publicize)
        }
    }

    /**
     * Publicize methods if they are public in [reference].
     *
     * @param reference The class to check the [AccessFlags] of the methods in.
     * @param logger A logger.
     */
    private fun ClassDef.fixMethodAccess(reference: ClassDef, logger: Logger? = null): ClassDef {
        val brokenMethods = methods.filterAny(reference.methods) { method, referenceMethod ->
            if (!MethodUtil.methodSignaturesMatch(method, referenceMethod)) return@filterAny false

            referenceMethod.accessFlags.isPublic() && !method.accessFlags.isPublic()
        }

        if (brokenMethods.isEmpty()) return this

        logger?.trace("Found ${brokenMethods.size} methods")

        /**
         * Make a method public.
         */
        fun MutableMethod.publicize() {
            accessFlags = accessFlags.toPublic()
        }

        return asMutableClass().apply {
            methods.filter { brokenMethods.contains(it) }.forEach(MutableMethod::publicize)
        }
    }

    private object Utils {
        fun ClassDef.asMutableClass() = if (this is MutableClass) this else this.toMutable()

        /**
         * Check if the [AccessFlags.PUBLIC] flag is set.
         *
         * @return True, if the flag is set.
         */
        fun Int.isPublic() = AccessFlags.PUBLIC.isSet(this)

        /**
         * Make [AccessFlags] public.
         *
         * @return The new [AccessFlags].
         */
        fun Int.toPublic() = this.or(AccessFlags.PUBLIC).and(AccessFlags.PRIVATE.value.inv())

        /**
         * Filter [this] on [needles] matching the given [predicate].
         *
         * @param this The hay to filter for [needles].
         * @param needles The needles to filter [this] with.
         * @param predicate The filter.
         * @return The [this] filtered on [needles] matching the given [predicate].
         */
        fun <HayType, NeedleType> Iterable<HayType>.filterAny(
            needles: Iterable<NeedleType>, predicate: (HayType, NeedleType) -> Boolean
        ) = Iterable<HayType>::filter.any(this, needles, predicate)

        /**
         * Filter [this] on [needles] not matching the given [predicate].
         *
         * @param this The hay to filter for [needles].
         * @param needles The needles to filter [this] with.
         * @param predicate The filter.
         * @return The [this] filtered on [needles] not matching the given [predicate].
         */
        fun <HayType, NeedleType> Iterable<HayType>.filterNotAny(
            needles: Iterable<NeedleType>, predicate: (HayType, NeedleType) -> Boolean
        ) = Iterable<HayType>::filterNot.any(this, needles, predicate)

        fun <HayType, NeedleType> KFunction2<Iterable<HayType>, (HayType) -> Boolean, List<HayType>>.any(
            haystack: Iterable<HayType>,
            needles: Iterable<NeedleType>,
            predicate: (HayType, NeedleType) -> Boolean
        ) = this(haystack) { hay ->
            needles.any { needle ->
                predicate(hay, needle)
            }
        }
    }
}