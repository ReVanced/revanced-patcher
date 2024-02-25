<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="../assets/revanced-headline/revanced-headline-vertical-dark.svg"
    >
    <img 
      width="256px"
      src="../assets/revanced-headline/revanced-headline-vertical-light.svg"
    >
  </picture>
  <br>
  <a href="https://revanced.app/">
     <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="../assets/revanced-logo/revanced-logo.svg" />
         <img height="24px" src="../assets/revanced-logo/revanced-logo.svg" />
     </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://github.com/ReVanced">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://i.ibb.co/dMMmCrW/Git-Hub-Mark.png" />
           <img height="24px" src="https://i.ibb.co/9wV3HGF/Git-Hub-Mark-Light.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="http://revanced.app/discord">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032563-d4e084b7-244e-4358-af50-26bde6dd4996.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://reddit.com/r/revancedapp">
       <picture>
           <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
           <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032351-9d9d5619-8ef7-470a-9eec-2744ece54553.png" />
       </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://t.me/app_revanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032213-faf25ab8-0bc3-4a94-a730-b524c96df124.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://x.com/revancedapp">
      <picture>
         <source media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/93124920/270180600-7c1b38bf-889b-4d68-bd5e-b9d86f91421a.png">
         <img height="24px" src="https://user-images.githubusercontent.com/93124920/270108715-d80743fa-b330-4809-b1e6-79fbdc60d09c.png" />
      </picture>
   </a>&nbsp;&nbsp;&nbsp;
   <a href="https://www.youtube.com/@ReVanced">
      <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
         <img height="24px" src="https://user-images.githubusercontent.com/13122796/178032714-c51c7492-0666-44ac-99c2-f003a695ab50.png" />
     </picture>
   </a>
   <br>
   <br>
   Continuing the legacy of Vanced
</p>

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

Continue: [🧩 Introduction to ReVanced Patches](2_patches_intro.md)
