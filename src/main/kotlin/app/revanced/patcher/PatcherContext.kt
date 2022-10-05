package app.revanced.patcher

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.Context
import app.revanced.patcher.data.PackageMetadata
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.Patch
import org.jf.dexlib2.iface.ClassDef
import java.io.File

data class PatcherContext(
    val classes: MutableList<ClassDef>,
    val resourceCacheDirectory: File,
) {
    val packageMetadata = PackageMetadata()
    internal val patches = mutableListOf<Class<out Patch<Context>>>()
    internal val bytecodeContext = BytecodeContext(classes)
    internal val resourceContext = ResourceContext(resourceCacheDirectory)
}