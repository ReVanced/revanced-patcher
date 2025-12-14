package collections

internal expect fun <K, V> MutableMap<K, V>.kmpMerge(
    key: K,
    value: V,
    remappingFunction: (oldValue: V, newValue: V) -> V,
)

internal fun <K, V> MutableMap<K, V>.merge(
    key: K,
    value: V,
    remappingFunction: (oldValue: V, newValue: V) -> V,
) = kmpMerge(key, value, remappingFunction)