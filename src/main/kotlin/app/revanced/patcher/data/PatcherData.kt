package app.revanced.patcher.data

import app.revanced.patcher.data.base.Data
import app.revanced.patcher.data.implementation.BytecodeData
import app.revanced.patcher.data.implementation.ResourceData
import app.revanced.patcher.patch.base.Patch
import org.jf.dexlib2.iface.ClassDef
import java.io.File

data class PatcherData(
    internal val internalClasses: MutableList<ClassDef>,
    internal val resourceCacheDirectory: String,
    val packageMetadata: PackageMetadata
) {

    internal val patches = mutableListOf<Class<out Patch<Data>>>()

    internal val bytecodeData = BytecodeData(internalClasses)
    internal val resourceData = ResourceData(File(resourceCacheDirectory))
}