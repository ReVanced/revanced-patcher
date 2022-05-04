package app.revanced.patcher.data

import app.revanced.patcher.data.base.Data
import app.revanced.patcher.data.implementation.BytecodeData
import app.revanced.patcher.data.implementation.ResourceData
import app.revanced.patcher.patch.base.Patch
import org.jf.dexlib2.iface.ClassDef
import java.io.File

internal data class PatcherData(
    val internalClasses: MutableList<ClassDef>,
    val resourceCacheDirectory: String
) {
    internal val patches = mutableListOf<Patch<Data>>()

    internal val bytecodeData = BytecodeData(patches, internalClasses)
    internal val resourceData = ResourceData(File(resourceCacheDirectory))
}