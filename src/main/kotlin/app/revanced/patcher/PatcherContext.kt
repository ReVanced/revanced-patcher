package app.revanced.patcher

import app.revanced.patcher.patch.Patch
import org.jf.dexlib2.iface.ClassDef
import java.io.File

data class PatcherContext(
    val classes: MutableList<ClassDef>,
    private val resourceCacheDirectory: File,
) {
    internal val patches = mutableListOf<Class<out Patch<Context>>>()
    internal val bytecodeContext = BytecodeContext(classes)
    internal val resourceContext = ResourceContext(resourceCacheDirectory)
}
