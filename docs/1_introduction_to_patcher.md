# 💉 Introduction to ReVanced Patcher

In order to create patches for Android applications, you first need to understand the fundamentals of ReVanced Patcher.

## 📙 How it works

ReVanced Patcher is a library that allows you to modify Android applications by applying patches to their APKs. It is built on top of [Smali](https://github.com/google/smali) for bytecode manipulation and [Androlib (Apktool)](https://github.com/iBotPeaches/Apktool) for resource decoding and encoding.
ReVanced Patcher accepts a list of patches and integrations, and applies them to a given APK file. It then returns the modified components of the APK file, such as modified dex files and resources, that can be repackaged into a new APK file.

ReVanced Patcher has a simple API that allows you to load patches and integrations from JAR files and apply them to an APK file.
Later on, you will learn how to create patches.

```kt
 // Executed patches do not necessarily reset their state.
 // For that reason it is important to create a new instance of the PatchBundleLoader
 // once the patches are executed instead of reusing the same instance of patches loaded by PatchBundleLoader.
val patches: PatchSet /* = Set<Patch<*>> */ = PatchBundleLoader.Jar(File("revanced-patches.jar"))
val integrations = setOf(File("integrations.apk"))

// Instantiating the patcher will decode the manifest of the APK file to read the package and version name.
val patcherConfig = PatcherConfig(apkFile = File("some.apk"))
val patcherResult = Patcher(patcherConfig).use { patcher ->
    patcher.apply {
        acceptIntegrations(integrations)
        acceptPatches(patches)

        // Execute patches.
        runBlocking {
            patcher.apply(returnOnError = false).collect { patchResult ->
                if (patchResult.exception != null)
                    println("${patchResult.patchName} failed:\n${patchResult.exception}")
                else
                    println("${patchResult.patchName} succeeded")
            }
        }
    }.get()
}

// The result of the patcher contains the modified components of the APK file that can be repackaged into a new APK file.
val dexFiles = patcherResult.dexFiles
val resources = patcherResult.resources
```

## ⏭️ What's next

The next page teaches the fundamentals of ReVanced Patches.

Continue: [🧩 Introduction to ReVanced Patches](2_introduction_to_patches.md)
