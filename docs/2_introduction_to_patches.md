# 🧩 Introduction to ReVanced Patches

Learn the basic concepts of ReVanced Patcher and how to create patches.

## 📙 Fundamentals

A patch is a piece of code that modifies an Android application.  
There are multiple types of patches. Each type can modify a different part of the APK such as the Dalvik VM bytecode, the APK resources, or arbitrary files in the APK:

- A `BytecodePatch` modifies the Dalvik VM bytecode
- A `ResourcePatch` modifies (decoded) resources
- A `RawResourcePatch` modifies arbitrary files

Each patch can declare a set of dependencies on other patches. ReVanced Patcher will first execute dependencies before executing the patch itself. This way, multiple patches can work together for abstract purposes in a modular way.

A patch class can be annotated with `@Patch` to provide metadata about and dependencies of the patch.
Alternatively, a constructor of the super class can be used. This is useful in the example scenario where you want to create an abstract patch class.

The entry point of a patch is the `execute` functions. This function is called by ReVanced Patcher when the patch is executed. The `execute` function receives an instance of the context object that provides access to the APK. The patch can use this context to modify the APK.

Each type of context provides different APIs to modify the APK. For example, the `BytecodeContext` provides APIs to modify the Dalvik VM bytecode, while the `ResourceContext` provides APIs to modify resources.

The difference between `ResourcePatch` and `RawResourcePatch` is that ReVanced Patcher will decode the resources if it is supplied a `ResourcePatch` for execution or if any kind of patch depends on a `ResourcePatch` and will not decode the resources before executing `RawResourcePatch`. Both, `ResourcePatch` and `RawResourcePatch` can modify arbitrary files in the APK whereas only `ResourcePatch` can modify decoded resources. The choice of which type to use depends on the use case. Decoding and building resources is a time- and resource-consuming process, so if the patch does not need to modify decoded resources, it is better to use `RawResourcePatch` or `BytecodePatch`.

Example of a `BytecodePatch`:

```kt
@Surpress("unused")
object MyPatch : BytecodePatch() {
	override fun execute(context: BytecodeContext) {
		// Your patch code here
	}
}
```

Example of a `ResourcePatch`:

```kt
@Surpress("unused")
object MyPatch : ResourcePatch() {
	override fun execute(context: ResourceContext) {
		// Your patch code here
	}
}
```

Example of a `RawResourcePatch`:

```kt
@Surpress("unused")
object MyPatch : RawResourcePatch() {
	override fun execute(context: ResourceContext) {
		// Your patch code here
	}
}
```

> [!TIP]
> To see real-world examples of patches, check out the [ReVanced Patches](https://github.com/revanced/revanced-patches) repository.

## ⏭️ Whats next

The next page will guide you through the setup of a development environment for creating patches.

Continue: [👶 Setup of a development environment](2_1_setup.md)
