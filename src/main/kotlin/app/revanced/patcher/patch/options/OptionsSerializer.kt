package app.revanced.patcher.patch.options

/**
 * Basic interface for serializing and deserializing of [PatchOption]s.
 *
 * Implementation note:
 * [OptionsContainer.key] must be used as the root key for all options of a container.
 * [PatchOption.key] must be used as the key for each option, which must be a child of the root key.
 *
 * @param SerializedData The type of the serialized data.
 */
interface OptionsSerializer<SerializedData> {
    fun serialize(containers: List<OptionsContainer>): SerializedData
    fun deserialize(containers: List<OptionsContainer>, serialized: SerializedData)
}