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

# 🧩 Anatomy of a ReVanced patch

Learn the API to create patches using ReVanced Patcher.

## ⛳️ Example patch

```kt
package app.revanced.patches.ads

val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Disable ads in the app.",
) { 
    compatibleWith { 
        "com.some.app"("1.0.0")
    }

    dependsOn { 
        disableAdsResourcePatch()
    }
    
    val showAdsFingerprintResult by methodFingerprint {
        // ...
    }
    
    execute {
        showAdsFingerprintResult.mutableMethod.addInstructions(
            0,
            """
                # Return false.
                const/4 v0, 0x0
                return v0
            """
        )
    }
}
```

> [!NOTE]  
>
> - Patches do not require a name, but `PatchLoader` will only load named patches.
> - Patches can depend on others. Dependencies are executed first.
> The dependent patch will not be executed if a dependency raises an exception.
> - A patch can declare compatibility with specific packages and versions, but patches can still be executed on any package or version. It is recommended to declare explicit compatibility to list known compatible packages.
>   - If `compatibleWith` is not called, the patch is compatible with any package
>   - If a package is specified with no versions, the patch is compatible with any version of the package
>   - If an empty array of versions is specified, the patch is not compatible with any version of the package. This is useful for declaring explicit incompatibility with a specific package.
> - This patch uses a fingerprint to find the method and replaces the method's instructions with new instructions.
> The fingerprint is resolved on the classes present in `BytecodePatchContext`.
> Fingerprints will be explained in more detail on the next page.
> - A patch can raise a `PatchException` at any time to indicate that the patch failed to execute. Any other `Exception` or `Throwable` raised will be wrapped in a `PatchException`.

> [!WARNING]
>
> - Circular dependencies are not allowed. If a patch depends on another patch, the other patch cannot depend on the first patch.
> - Dependencies inherit compatibility from dependant patches.


> [!TIP]
> To see real-world examples of patches, check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## 🧩 Patch API

### ♻️ Finalization

Patches can have a finalization block called after all patches have been executed, in reverse order of patch execution.

```kt
val patch = bytecodePatch(name = "Patch") { 
    dependsOn { 
        bytecodePatch(name = "Dependency") { 
            execute {
                print("1")
            }

            finalize {
                print("4")
            }
        }
    }

    execute {
        print("2")
    }

    finalize {
        print("3")
    }
}
```

Because `Patch` depends on `Dependency`, first `Dependency` is executed, then `Patch`. The finalization blocks are called in reverse order of patch execution, which means, first, the finalization block of `Patch`, then the finalization block of `Dependency` is called. The output of the above patch would be `1234`. The same order is followed for multiple patches depending on the patch.

### ⚙️ Patch options

Patches can have options to get and set before a patch is executed. Multiple inbuilt types can be used as options.

To define an option, use available `option` functions:

```kt
val patch = bytecodePatch(name = "Patch") {
    // Add an option with a custom type and delegate it's value to a variable.
    val string by option<String>(key = "string")

    // Add an inbuilt option and delegate it's value to a variable.
    val value by stringOption(key = "option")
    
    execute {
        println(string)
        println(value)
    }
}
```

Options of a patch can be set after loading the patches with `PatchLoader` by obtaining the instance for the patch:

```kt
loadPatchesJar(patchesJarFile).apply {
    // Type is checked at runtime.
    first { it.name == "Patch" }.options["option"] = "Value"
}
```

The type of an option can be obtained from the `type` property of the option:

```kt
option.type // The KType of the option.
```

## ⏭️ What's next

The next page explains the concept of fingerprinting in ReVanced Patcher.

Continue: [🔎 Fingerprinting](2_2_1_fingerprinting.md)
