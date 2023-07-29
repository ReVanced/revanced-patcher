package app.revanced.arsc.xml

import app.revanced.arsc.resource.ResourceContainer
import app.revanced.arsc.resource.boolean
import com.reandroid.apk.xmlencoder.EncodeException
import com.reandroid.apk.xmlencoder.XMLEncodeSource
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.xml.XMLDocument
import com.reandroid.xml.XMLElement
import com.reandroid.xml.source.XMLDocumentSource

/**
 * Archive input source to lazily encode an [XMLDocument] after it has been modified.
 *
 * @param name The file name of this input source.
 * @param document The [XMLDocument] to encode.
 * @param resources The [ResourceContainer] to use for encoding.
 */
internal class LazyXMLEncodeSource(
    name: String,
    val document: XMLDocument,
    private val resources: ResourceContainer
) : XMLEncodeSource(resources.resourceTable.encodeMaterials, XMLDocumentSource(name, document)) {
    private var encoded = false

    override fun getResXmlBlock(): ResXmlDocument {
        if (encoded) return super.getResXmlBlock()

        XMLEncodeSource(resources.resourceTable.encodeMaterials, XMLDocumentSource(name, document))
        
        fun XMLElement.registerIds() {
            listAttributes().forEach { attr ->
                if (!attr.value.startsWith("@+id/")) return@forEach

                val name = attr.value.split('/').last()
                resources.setResource("id", name, boolean(false))
                attr.value = "@id/$name"
            }

            listChildElements().forEach { it.registerIds() }
        }

        // Handle all @+id/id_name references in the document.
        document.documentElement.registerIds()

        encoded = true

        // This will call XMLEncodeSource.getResXmlBlock(),
        // which will encode the document if it has not already been encoded.
        try {
            return super.getResXmlBlock()
        } catch (e: EncodeException) {
            throw EncodeException("Failed to encode $name", e)
        }
    }
}