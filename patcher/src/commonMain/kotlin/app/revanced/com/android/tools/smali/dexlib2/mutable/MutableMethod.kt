package app.revanced.com.android.tools.smali.dexlib2.mutable

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableAnnotation.Companion.toMutable
import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethodParameter.Companion.toMutable
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.Method

class MutableMethod(
    method: Method,
) : BaseMethodReference(),
    Method {
    private var definingClass = method.definingClass
    private var name = method.name
    private var accessFlags = method.accessFlags
    private var returnType = method.returnType

    // TODO: Create own mutable MethodImplementation (due to not being able to change members like register count).
    private var implementation = method.implementation?.let(::MutableMethodImplementation)
    private val _annotations by lazy { method.annotations.map { annotation -> annotation.toMutable() }.toMutableSet() }
    private val _parameters by lazy { method.parameters.map { parameter -> parameter.toMutable() }.toMutableList() }
    private val _parameterTypes by lazy { method.parameterTypes.toMutableList() }
    private val _hiddenApiRestrictions by lazy { method.hiddenApiRestrictions }

    fun setDefiningClass(definingClass: String) {
        this.definingClass = definingClass
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setAccessFlags(accessFlags: Int) {
        this.accessFlags = accessFlags
    }

    fun setReturnType(returnType: String) {
        this.returnType = returnType
    }

    fun setImplementation(implementation: MutableMethodImplementation?) {
        this.implementation = implementation
    }

    override fun getDefiningClass() = definingClass

    override fun getName() = name

    override fun getParameterTypes() = _parameterTypes

    override fun getReturnType() = returnType

    override fun getAnnotations() = _annotations

    override fun getAccessFlags() = accessFlags

    override fun getHiddenApiRestrictions() = _hiddenApiRestrictions

    override fun getParameters() = _parameters

    override fun getImplementation() = implementation

    companion object {
        fun Method.toMutable() = MutableMethod(this)
    }
}
