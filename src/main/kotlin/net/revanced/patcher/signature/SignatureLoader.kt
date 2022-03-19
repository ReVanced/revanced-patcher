package net.revanced.patcher.signature

import com.google.gson.Gson
import net.revanced.patcher.signature.model.Signature

object SignatureLoader {
    private val gson = Gson()

    fun LoadFromJson(json: String): Array<Signature> {
        return gson.fromJson(json, Array<Signature>::class.java)
    }
}
