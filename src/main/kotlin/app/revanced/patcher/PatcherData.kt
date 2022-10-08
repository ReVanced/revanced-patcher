
import app.revanced.patcher.data.Data
import app.revanced.patcher.data.impl.BytecodeData
import app.revanced.patcher.data.impl.ResourceData
import app.revanced.patcher.patch.Patch
import org.jf.dexlib2.iface.ClassDef
import java.io.File

data class PatcherData(
    val internalClasses: MutableList<ClassDef>,
    val resourceCacheDirectory: String,
) {
    internal val patches = mutableListOf<Class<out Patch<Data>>>()
    internal val bytecodeData = BytecodeData(internalClasses)
    internal val resourceData = ResourceData(File(resourceCacheDirectory))
}