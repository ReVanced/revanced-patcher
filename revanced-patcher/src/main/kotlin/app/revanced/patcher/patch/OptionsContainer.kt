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
    open val options = PatchOptions()

    /**
     * Registers a [PatchOption].
     * @param opt The [PatchOption] to register.
     * @return The registered [PatchOption].
     */
    protected fun <T> option(opt: PatchOption<T>): PatchOption<T> {
        options.register(opt)
        return opt
    }
}