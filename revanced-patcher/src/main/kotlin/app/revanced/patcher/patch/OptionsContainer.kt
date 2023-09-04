package app.revanced.patcher.patch

/**
 * A container for patch options.
 */
abstract class OptionsContainer {
    /**
     * A list of [PatchOption]s.
     * @see PatchOptions
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val options = PatchOptions()

    protected fun <T> option(opt: PatchOption<T>): PatchOption<T> {
        options.register(opt)
        return opt
    }
}