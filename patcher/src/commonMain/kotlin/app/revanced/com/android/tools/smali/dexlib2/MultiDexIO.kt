package app.revanced.com.android.tools.smali.dexlib2

import com.android.tools.smali.dexlib2.DexFileFactory
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.writer.DexWriter
import com.android.tools.smali.dexlib2.writer.io.FileDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal fun readMultiDex(input: File): ReadResult {
    val container = DexFileFactory.loadDexContainer(input, null)

    // Each entry can have up to DexWriter.MAX_POOL_SIZE classes,
    // so pre-size the set to avoid resizing as we add classes.
    val classDefsCount = (container.dexEntryNames.size) * DexWriter.MAX_POOL_SIZE
    val classDefs = HashSet<ClassDef>(classDefsCount)

    var opcodes: Opcodes? = null

    container.dexEntryNames.map { container.getEntry(it)!!.dexFile }.flatMapTo(classDefs) {
        if (opcodes == null || it.opcodes.api > opcodes!!.api) opcodes = it.opcodes

        it.classes
    }

    return ReadResult(classDefs, opcodes ?: Opcodes.getDefault())
}

internal fun writeMultiDex(
    outputDir: File,
    classDefs: Iterator<ClassDef?>,
    opcodes: Opcodes,
    threadCount: Int,
    emitDexFile: ((Int, File) -> Unit) = { _, _ -> }
): Int {
    val threadCount = if (threadCount <= 0) {
        val processors = Runtime.getRuntime().availableProcessors()
        if (processors > 0) processors else 1
    } else threadCount

    val executor = Executors.newFixedThreadPool(threadCount)
    val inFlight = ArrayDeque<PendingWrite>(threadCount)
    var carry: ClassDef? = null
    var dexIndex = 1

    try {
        while (carry != null || classDefs.hasNext()) {
            val chunk = buildWriteChunk(classDefs, carry, opcodes)
            if (chunk.classCount == 0) break

            val currentDexIndex = dexIndex++

            val outputFile = File(
                outputDir,
                if (currentDexIndex == 1) "classes.dex" else "classes$currentDexIndex.dex"
            )

            val future = executor.submit {
                chunk.dexPool.writeTo(FileDataStore(outputFile))
            }

            inFlight.addLast(PendingWrite(currentDexIndex, outputFile, future))

            carry = chunk.carry

            if (inFlight.size >= threadCount) waitForWrite(emitDexFile, inFlight)
        }

        while (!inFlight.isEmpty()) waitForWrite(emitDexFile, inFlight)
    } catch (e: InterruptedException) {
        throw InterruptedIOException().apply { initCause(e) }
    } finally {
        executor.shutdownNow()
    }

    return dexIndex - 1
}

private fun waitForWrite(
    emitDexFile: ((Int, File) -> Unit),
    inFlight: ArrayDeque<PendingWrite>
) {
    val pendingWrite = inFlight.removeFirst()

    try {
        pendingWrite.future.get()
    } catch (e: ExecutionException) {
        when (val cause = e.cause) {
            is IOException, is RuntimeException, is Error -> throw cause
            else -> throw IOException("Unexpected exception while writing dex", cause)
        }
    }

    emitDexFile(pendingWrite.dexIndex, pendingWrite.outputFile)
}

private fun buildWriteChunk(
    classIterator: Iterator<ClassDef?>,
    carry: ClassDef?,
    opcodes: Opcodes
): WriteChunk {
    val dexPool = DexPool(opcodes)
    var classCount = 0
    var carryClass = carry

    while (true) {
        val classDef: ClassDef?

        if (carryClass != null) {
            classDef = carryClass
            carryClass = null
        } else if (classIterator.hasNext()) classDef = classIterator.next()
        else break

        dexPool.apply {
            mark()
            internClass(classDef)
            if (hasOverflowed(DexWriter.MAX_POOL_SIZE)) {
                reset()

                if (classCount == 0) throw IOException("Type too big for dex pool: " + classDef!!.type)
                carryClass = classDef

                break
            }
        }

        classCount++
    }

    return WriteChunk(dexPool, classCount, carryClass)
}

class ReadResult internal constructor(val classDefs: MutableSet<ClassDef>, val opcodes: Opcodes)

private class WriteChunk(val dexPool: DexPool, val classCount: Int, val carry: ClassDef?)

private class PendingWrite(
    val dexIndex: Int,
    val outputFile: File,
    val future: Future<*>
)

internal class NullingArrayIterator<T> (private var items: Array<T?>?) :
    MutableIterator<T?> {
    private var index = 0

    override fun hasNext() = items != null && index < items!!.size

    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()

        val items = items!!

        val item = items[index]
        items[index] = null

        index++

        if (index >= items.size) this.items = null // Help GC.

        return item!!
    }

    override fun remove() = throw UnsupportedOperationException()
}

internal inline fun <reified T> nullingArrayIteratorOf(items: Collection<T>) =
    NullingArrayIterator(items.toTypedArray<T?>())
