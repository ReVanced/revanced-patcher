package app.revanced.patcher.patch.options

import kotlin.reflect.KProperty

private val KEY_REGEX = Regex("^[a-zA-Z0-9_]+$")

@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class PatchOption<T> internal constructor(
    val key: String,
    val title: String,
    val description: String,
    defaultValue: T,
) {
    init {
        require(key.isNotBlank()) { "Key cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(key.matches(KEY_REGEX)) { "Key may only contain alphanumeric characters, underscores and dots" }
    }

    internal var value: T = defaultValue

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        value = newValue
    }
}