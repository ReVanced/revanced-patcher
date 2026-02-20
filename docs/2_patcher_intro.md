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

ReVanced Patcher is made out of three structural components.

## 📙 How it works

ReVanced Patcher is a library that makes modifying Android apps easy and modular by applying patches.
It is built on top of [Smali](https://github.com/google/smali) for bytecode manipulation and [Androlib (Apktool)](https://github.com/iBotPeaches/Apktool)
for resource decoding and encoding.

First, you load a set of patches to filter as desired based on their attributes:

```kt
val patches = loadPatches(File("revanced-patches.rvp"))
```

Afterward, you create a patcher instance for a specific APK file.
In the `getPatches` lambda, you receive the packageName and versionName of the APK file being patched.
This is useful to decide which patches to return to the patcher based on the target application:

```kt
val patch = patcher(apkFile = File("app.apk")) { packageName, versionName ->
    patches // Optionally filter this set - for example based on packageName and versionName.
}
```

The `patcher` function returns a `patch` function that you can later execute to apply the patches to the APK file.
On execution, `PatchResult` objects for each patch are emitted in the provided lambda.
You can use this to log the success or failure of each patch.
The `patch` function finally returns a `patchesResult` containing the modified components of the APK file,
ready for repackaging:

```kt
val patchesResult = patch { patchResult ->
    val exception = patchResult.exception
        ?: return@patch logger.info("\"${patchResult.patch}\" succeeded")

    StringWriter().use { writer ->
        exception.printStackTrace(PrintWriter(writer))

        logger.severe("\"${patchResult.patch}\" failed:\n$writer")
    }
}

val dexFiles = patchesResult.dexFiles
val resources = patchesResult.resources
```


## ⏭️ What's next

The next page introduces the basics of ReVanced Patches.

Continue: [🧩 Introduction to ReVanced Patches](3_patches_intro.md)
