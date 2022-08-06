package app.revanced.patcher.util.patch.bundle

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class PatchBundleFormatTest {
    // REMINDER: update this when format is changed
    private val testData = byteArrayOf(
        *PatchBundleFormat.MAGIC,
        0x04, 0x54, 0x65, 0x73, 0x74,
        0x05, 0x31, 0x2e, 0x30, 0x2e,
        0x30, 0x06, 0x53, 0x63, 0x75,
        0x6c, 0x61, 0x73, 0x00
    )

    private val metadata = PatchBundle.Metadata(
        name = "Test",
        version = "1.0.0",
        authors = "Sculas"
    )

    @Test
    fun serialize() {
        val serialized = PatchBundleFormat.serialize(metadata, listOf())
        assertContentEquals(testData, serialized)
    }

    @Test
    fun serializeWithResources() {
        val (resourceName, resourceType) = "test.txt" to PatchResourceType.RESOURCE
        val testResource = resource(resourceName, resourceType)
        val serialized = PatchBundleFormat.serialize(metadata, listOf(testResource))
        val deserialized = PatchBundleFormat.deserialize(serialized)
        deserialized.resources.size.let { size ->
            assertTrue(size == 1, "Expected to find 1 resource, but found $size instead")
        }
        assertNotNull(deserialized.resources[resourceName])
        deserialized.resources[resourceName]!!.let { resource ->
            assertEquals(resourceName, resource.key)
            assertEquals(resourceType, resource.type)
        }
        assertContentEquals(testResource.data, deserialized.resources.streamOf(resourceName)!!.readAllBytes())
    }

    @Test
    fun deserialize() {
        val deserialized = PatchBundleFormat.deserialize(testData)
        assertEquals(metadata, deserialized.metadata)
    }

    @Test
    fun throwsMalformedBundleException() {
        assertMalformed(byteArrayOf(0))
        assertMalformed(PatchBundleFormat.MAGIC)
    }

    private companion object {
        fun resource(name: String, type: PatchResourceType) = PatchResource(
            name,
            type,
            PatchBundleFormatTest::class.java
                .getResourceAsStream("/$name")?.readAllBytes()
                ?: throw IllegalStateException("Missing test resource")
        )

        fun assertMalformed(data: ByteArray) = assertThrows<MalformedBundleException> {
            PatchBundleFormat.deserialize(data)
        }
    }
}