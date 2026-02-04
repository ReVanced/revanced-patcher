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

ReVanced Patches offer a couple APIs to create patches for Android applications.

## ⛳️ Example patch

The following example demonstrates how to disable ads in an app:

```kt
package app.revanced.patches.com.some.app

val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Disable ads in the app.",
) {
    compatibleWith("com.some.app"("1.0.0"))

    // Patches can depend on other patches, applying them first.
    dependsOn(disableAdsResourcePatch)

    // Merge precompiled DEX files into the patched app, before the patch is applied.
    extendWith("disable-ads.rve")

    // Business logic of the patch to disable ads in the app.
    apply {
        // Find the method to patch.
        val showAdsMethod = firstMethod {
            // More about this on the next page of the documentation.
        }

        // DisableAdsPatch.shouldDisableAds() is available from the `disable-ads.rve` extension (precompiled DEX file).
        showAdsMethod.addInstructions(
            0,
            """
                invoke-static {}, LDisableAdsPatch;->shouldDisableAds()Z
                move-result v0
                return v0
            """
        )
    }
}
```

> [!TIP]
> To see real-world examples of patches,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## 🧩 Patch API

### ⚙️ Patch options

Patch options allow parametrizing a patch before applying it.
After loading the patches, options can be set for a patch.
Multiple types of options are already built into ReVanced Patcher. 
To define an option, use the available `option` functions:

```kt
val patch = bytecodePatch(name = "Patch") {
    // Add an inbuilt option and delegate its value to a property.
    val value by stringOption(name = "String option")

    // Add an option with a custom type and delegate its value to a property.
    val someValue by option<SomeType>(name = "Some type option")

    apply {
        println(value)
        println(someValue)
    }
}
```

To set the options of a patch, access the `options` map of the patch after loading it:

```kt
loadPatches(patches).apply {
    // Type is checked at runtime.
    first { it.name == "Patch" }.options["String option"] = "Value"
}
```

The type of option can be obtained from the `type` property of the option:

```kt
option.type // The KType of the option. Captures the full type information of the option, including generics.
```

Options can be declared outside a patch and added to a patch afterward by invoking them inside the patch.
This is useful when you want to use same option in multiple patches directly:

```kt
val option = stringOption(name = "Option")

bytecodePatch(name = "Patch") {
    val value by option()
}

bytecodePatch(name = "Another patch") {
    val value by option()
}
```


### 🧩 Extensions

Extensions are precompiled DEX file merged into the `BytecodePatchContext` before a patch is applied.
While patches deal with the compile-time side of apps, extensions take care of the apps runtime 
by extending the patched app with additional classes.

Assume you want to add a complex feature to an app that would need multiple classes and methods:

```java
public class ComplexPatch {
    public static void doSomething() {
        // ...
    }
}
```

After compiling the above code as a DEX file, you can add the DEX file as a resource in the patches file
and use it in a patch:

```kt
val patch = bytecodePatch(name = "Complex patch") {
    extendWith("complex-patch.rve")

    apply {
        someMethod.addInstructions(0, "invoke-static { }, LComplexPatch;->doSomething()V")
    }
}
```

Alternatively, you can use another overload of `extendWith` to provide your own `InputStream` to an extension:

```kt
extendWith { File("complex-patch.rve").inputStream() }
```

Before calling the `apply` block of the patch,
the classes from the extension are merged into `BytecodePatchContext.classDefs`.
When the patch is applied, it can reference the classes and methods from the extension.

> [!NOTE]
>
> The [ReVanced Patches template](https://github.com/ReVanced/revanced-patches-template) repository
> is a template project to create patches and extensions.

> [!TIP]
> To see real-world examples of extensions,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

### ♻️ The `afterDependents` block

Patches can define an `afterDependents` block, which is called after all dependent patches have been applied.
This is useful for post-processing the work of dependent patches.

```kt
val patch = bytecodePatch(name = "Patch") {
    dependsOn(
        bytecodePatch(name = "Dependency") {
            apply {
                print("1")
            }

          afterDependents {
            print("4")
          }
        }
    )

    apply {
        print("2")
    }

    afterDependents {
        print("3")
    }
}
```

Because `Patch` depends on `Dependency`, first `Dependency` is applied, then `Patch`.
The `afterDependents` blocks are called in reverse order, which means,
first, the `afterDependents` block of `Patch`, then the `afterDependents` block of `Dependency` is called.
The output after applying the patch above would be `1234`.
The same order is followed for multiple patches depending on the patch.

## 💡 Additional tips

- When using `loadPatches` to load patches, only patches with a name are loaded.
  Refer to the inline documentation of `loadPatches` for detailed information.
- Patches can depend on others. Dependencies are applied first.
  The dependent patch will not be applied if a dependency raises an exception during application.
- A patch can declare compatibility with specific packages and versions,
  but it patches can still be applied on any package or version, if provided to the patcher.
  It is recommended that compatibility is specified to present known compatible packages and versions.
    - If `compatibleWith` is not used, the patch is treated as compatible with any package
- If a package is specified with no versions, the patch is compatible with any version of the package
- If an empty array of versions is specified, the patch is not compatible with any version of the package.
  This is useful for declaring incompatibility with a specific package.
- A patch can raise a `PatchException` inside `apply` to indicate that the patch failed to apply.

## ⏭️ What's next

The next page explains the concept of fingerprinting in ReVanced Patcher.

Continue: [🔎 Matching](3_2_matching.md)
