package app.revanced.patcher.cache.proxy.mutableTypes

import app.revanced.patcher.cache.proxy.mutableTypes.MutableAnnotation.Companion.toMutable
import app.revanced.patcher.cache.proxy.mutableTypes.MutableMethodParameter.Companion.toMutable
import org.jf.dexlib2.base.reference.BaseMethodReference
import org.jf.dexlib2.builder.MutableMethodImplementation
import org.jf.dexlib2.iface.Method

class MutableMethod(method: Method) : Method, BaseMethodReference() {
    private var definingClass = method.definingClass
    private var name = method.name
    private var accessFlags = method.accessFlags
    private var returnType = method.returnType

    // Create own mutable MethodImplementation (due to not being able to change members like register count)
    private var implementation = method.implementation?.let { MutableMethodImplementation(it) }
    private val annotations = method.annotations.map { annotation -> annotation.toMutable() }.toMutableSet()
    private val parameters = method.parameters.map { parameter -> parameter.toMutable() }.toMutableList()
    private val parameterTypes = method.parameterTypes.toMutableList()

    override fun getDefiningClass(): String {
        return this.definingClass
    }

    override fun getName(): String {
        return name
    }

    override fun getParameterTypes(): MutableList<CharSequence> {
        return parameterTypes
    }

    override fun getReturnType(): String {
        return returnType
    }

    override fun getAnnotations(): MutableSet<MutableAnnotation> {
        return annotations
    }

    override fun getAccessFlags(): Int {
        return accessFlags
    }

    override fun getParameters(): MutableList<MutableMethodParameter> {
        return parameters
    }

    override fun getImplementation(): MutableMethodImplementation? {
        return implementation
    }

    companion object {
        fun Method.toMutable(): MutableMethod {
            return MutableMethod(this)
        }
    }
}
