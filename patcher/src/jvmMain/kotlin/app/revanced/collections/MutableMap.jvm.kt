package app.revanced.collections

internal actual fun <K, V> MutableMap<K, V>.kmpMerge(
    key: K,
    value: V,
    remappingFunction: (oldValue: V, newValue: V) -> V,
) = MutableMap<K, V>::merge.call(key, value, remappingFunction) as Unit
