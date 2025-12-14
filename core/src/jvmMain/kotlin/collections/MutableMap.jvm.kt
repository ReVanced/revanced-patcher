package collections

internal actual fun <K, V> MutableMap<K, V>.kmpMerge(
    key: K,
    value: V,
    remappingFunction: (oldValue: V, newValue: V) -> V
) = merge(key, value, remappingFunction)