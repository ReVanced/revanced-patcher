package app.revanced.arsc.resource

import app.revanced.arsc.ApkResourceException
import com.reandroid.arsc.coder.EncodeResult
import com.reandroid.arsc.coder.ValueDecoder
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ValueType
import com.reandroid.arsc.value.array.ArrayBag
import com.reandroid.arsc.value.array.ArrayBagItem
import com.reandroid.arsc.value.plurals.PluralsBag
import com.reandroid.arsc.value.plurals.PluralsBagItem
import com.reandroid.arsc.value.plurals.PluralsQuantity
import com.reandroid.arsc.value.style.StyleBag
import com.reandroid.arsc.value.style.StyleBagItem

/**
 * A resource value.
 */
sealed class Resource {
    internal abstract fun write(entry: Entry, resources: ResourceContainer)
}

internal val Resource.complex get() = when (this) {
    is Scalar -> false
    is Complex -> true
}

/**
 * A simple resource.
 */
open class Scalar internal constructor(private val valueType: ValueType, private val value: Int) : Resource() {
    protected open fun data(resources: ResourceContainer) = value

    override fun write(entry: Entry, resources: ResourceContainer) {
        entry.setValueAsRaw(valueType, data(resources))
    }

    internal open fun toArrayItem(resources: ResourceContainer) = ArrayBagItem.create(valueType, data(resources))
    internal open fun toStyleItem(resources: ResourceContainer) = StyleBagItem.create(valueType, data(resources))
}

/**
 * A marker class for complex resources.
 */
sealed class Complex : Resource()

private fun encoded(encodeResult: EncodeResult?) = encodeResult?.let { Scalar(it.valueType, it.value) }
    ?: throw ApkResourceException.Encode("Failed to encode value")

/**
 * Encode a color.
 *
 * @param hex The hex value of the color.
 * @return The encoded [Resource].
 */
fun color(hex: String) = encoded(ValueDecoder.encodeColor(hex))

/**
 * Encode a dimension or fraction.
 *
 * @param value The dimension value such as 24dp.
 * @return The encoded [Resource].
 */
fun dimension(value: String) = encoded(ValueDecoder.encodeDimensionOrFraction(value))

/**
 * Encode a boolean resource.
 *
 * @param value The boolean.
 * @return The encoded [Resource].
 */
fun boolean(value: Boolean) = Scalar(ValueType.INT_BOOLEAN, if (value) -Int.MAX_VALUE else 0)

/**
 * Encode a float.
 *
 * @param n The number to encode.
 * @return The encoded [Resource].
 */
fun float(n: Float) = Scalar(ValueType.FLOAT, n.toBits())

/**
 * Create an integer [Resource].
 *
 * @param n The number to encode.
 * @return The integer [Resource].
 */
fun integer(n: Int) = Scalar(ValueType.INT_DEC, n)

/**
 * Create a reference [Resource].
 *
 * @param resourceId The target resource.
 * @return The reference resource.
 */
fun reference(resourceId: Int) = Scalar(ValueType.REFERENCE, resourceId)

/**
 * Resolve and create a reference [Resource].
 *
 * @see reference
 * @param ref The reference string to resolve.
 * @param resourceTable The resource table to resolve the reference with.
 * @return The reference resource.
 */
fun reference(resourceTable: ResourceTable, ref: String) = reference(resourceTable.resolve(ref))

/**
 * An array [Resource].
 *
 * @param elements The elements of the array.
 */
class Array(private val elements: Collection<Scalar>) : Complex() {
    override fun write(entry: Entry, resources: ResourceContainer) {
        ArrayBag.create(entry).addAll(elements.map { it.toArrayItem(resources) })
    }
}

/**
 * A style resource.
 *
 * @param elements The attributes to override.
 * @param parent A reference to the parent style.
 */
class Style(private val elements: Map<String, Scalar>, private val parent: String? = null) : Complex() {
    override fun write(entry: Entry, resources: ResourceContainer) {
        val resTable = resources.resourceTable
        val style = StyleBag.create(entry)
        parent?.let {
            style.parentId = resTable.resolve(parent)
        }

        style.putAll(
            elements.asIterable().associate {
                StyleBag.resolve(resTable.encodeMaterials, it.key) to it.value.toStyleItem(resources)
            })
    }
}

/**
 * A quantity string [Resource].
 *
 * @param elements A map of the quantity to the corresponding string.
 */
class Plurals(private val elements: Map<String, String>) : Complex() {
    override fun write(entry: Entry, resources: ResourceContainer) {
        val plurals = PluralsBag.create(entry)

        plurals.putAll(elements.asIterable().associate { (k, v) ->
            PluralsQuantity.value(k) to PluralsBagItem.string(resources.getOrCreateString(v))
        })
    }
}

/**
 * A string [Resource].
 *
 * @param value The string value.
 */
class StringResource(val value: String) : Scalar(ValueType.STRING, 0) {
    private fun tableString(resources: ResourceContainer) = resources.getOrCreateString(value)

    override fun data(resources: ResourceContainer) = tableString(resources).index
    override fun toArrayItem(resources: ResourceContainer) = ArrayBagItem.string(tableString(resources))
    override fun toStyleItem(resources: ResourceContainer) = StyleBagItem.string(tableString(resources))
}