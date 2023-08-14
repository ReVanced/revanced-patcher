package app.revanced.patcher

import app.revanced.patcher.data.*
import app.revanced.patcher.logging.Logger
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.util.ClassMerger.merge
import com.android.tools.smali.dexlib2.iface.ClassDef
import java.io.File

data class PatcherContext(
    val classes: MutableList<ClassDef>,
    val resourceCacheDirectory: File,
) {
    val packageMetadata = PackageMetadata()
    internal val patches = mutableListOf<Class<out Patch<Context>>>()
    internal val integrations = Integrations(this)
    internal val bytecodeContext = BytecodeContext(classes)
    internal val resourceContext = ResourceContext(resourceCacheDirectory)

    internal class Integrations(val context: PatcherContext) {
        var callback: ((File) -> Unit)? = null
        private val integrations: MutableList<File> = mutableListOf()

        fun add(integrations: List<File>) = this@Integrations.integrations.addAll(integrations)

        /**
         * Merge integrations.
         * @param logger A logger.
         */
        fun merge(logger: Logger) {
            with(context.bytecodeContext.classes) {
                for (integrations in integrations) {
                    callback?.let { it(integrations) }

                    for (classDef in lanchon.multidexlib2.MultiDexIO.readDexFile(
                        true,
                        integrations,
                        NAMER,
                        null,
                        null
                    ).classes) {
                        val type = classDef.type

                        val result = classes.findIndexed { it.type == type }
                        if (result == null) {
                            logger.trace("Merging type $type")
                            classes.add(classDef)
                            continue
                        }

                        val (existingClass, existingClassIndex) = result

                        logger.trace("Type $type exists. Adding missing methods and fields.")

                        existingClass.merge(classDef, context, logger).let { mergedClass ->
                            if (mergedClass !== existingClass) // referential equality check
                                classes[existingClassIndex] = mergedClass
                        }
                    }
                }
            }
        }
    }
}