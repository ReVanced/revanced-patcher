@file:Suppress("MemberVisibilityCanBePrivate")

package app.revanced.arsc.archive

import app.revanced.arsc.resource.ResourceContainer
import com.reandroid.apk.ApkModule
import com.reandroid.apk.DexFileInputSource
import com.reandroid.archive.InputSource
import java.io.File
import java.io.Flushable

/**
 * A class for reading/writing files in an [ApkModule].
 *
 * @param module The [ApkModule] to operate on.
 */
class Archive(internal val module: ApkModule) : Flushable {
    val mainPackageResources = ResourceContainer(this, module.tableBlock)

    fun save(output: File) {
        flush()
        module.writeApk(output)
    }
    fun readDexFiles(): MutableList<DexFileInputSource> = module.listDexFiles()
    fun write(inputSource: InputSource) = module.apkArchive.add(inputSource) // Overwrites existing files.
    fun read(name: String): InputSource? = module.apkArchive.getInputSource(name)
    override fun flush() = mainPackageResources.flush()
}