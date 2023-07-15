package app.revanced.patcher

import app.revanced.patcher.logging.Logger
import app.revanced.patcher.patch.PatchClass
import app.revanced.patcher.util.ClassMerger.merge
import lanchon.multidexlib2.MultiDexIO
import java.io.File

class PatcherContext(
    options: PatcherOptions,
    internal val patches: List<PatchClass>,
    integrations: Iterable<File>
) {
    internal val integrations = Integrations(this, integrations)
    internal val bytecodeContext = BytecodeContext(options.apkBundle)
    internal val resourceContext = ResourceContext(options.apkBundle)

    internal class Integrations(val context: PatcherContext, private val dexContainers: Iterable<File>) {
        var merge = false

        /**
         * Merge integrations.
         * @param logger A logger.
         */
        fun merge(logger: Logger) {
            context.bytecodeContext.classes.apply {
                for (integrations in dexContainers) {
                    logger.info("Merging $integrations")

                    for (classDef in MultiDexIO.readDexFile(true, integrations, Patcher.dexFileNamer, null, null).classes) {
                        val type = classDef.type

                        val existingClassIndex = this.indexOfFirst { it.type == type }
                        if (existingClassIndex == -1) {
                            logger.trace("Merging type $type")
                            add(classDef)
                            continue
                        }


                        logger.trace("Type $type exists. Adding missing methods and fields.")

                        get(existingClassIndex).apply {
                            merge(classDef, context.bytecodeContext, logger).let { mergedClass ->
                                if (mergedClass !== this) // referential equality check
                                    set(existingClassIndex, mergedClass)
                            }
                        }

                    }
                }
            }
        }
    }
}