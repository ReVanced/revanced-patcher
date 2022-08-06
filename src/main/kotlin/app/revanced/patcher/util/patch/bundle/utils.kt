package app.revanced.patcher.util.patch.bundle

open class MalformedBundleException(message: String) : Exception(message)

object IllegalValueException : MalformedBundleException("Illegal value read")
class EOFReachedException(n: Int) : MalformedBundleException("EOF reached before the requested $n bytes could be read")