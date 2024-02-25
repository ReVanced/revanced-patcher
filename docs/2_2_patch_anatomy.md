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

Throughout the documentation, the following example will be used to demonstrate the concepts of patches:

```kt
package app.revanced.patches.ads

@Patch(
    name = "Disable ads",
    description = "Disable ads in the app.",
    dependencies = [DisableAdsResourcePatch::class],
    compatiblePackages = [CompatiblePackage("com.some.app", ["1.3.0"])]
)
object DisableAdsPatch : BytecodePatch(
    setOf(ShowAdsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        ShowAdsFingerprint.result?.let { result ->
            result.mutableMethod.addInstructions(
                0,
                """
                    # Return false.
                    const/4 v0, 0x0
                    return v0
                """
            )
        } ?: throw PatchException("ShowAdsFingerprint not found")
    }
}
```

## 🔎 Breakdown

The example patch consists of the following parts:

### 📝 Patch annotation

```kt
@Patch(
    name = "Disable ads",
    description = "Disable ads in the app.",
    dependencies = [DisableAdsResourcePatch::class],
    compatiblePackages = [CompatiblePackage("com.some.app", ["1.3.0"])]
)
```

The `@Patch` annotation is used to provide metadata about the patch.

Notable annotation parameters are:

- `name`: The name of the patch. This is used as an identifier for the patch.
  If this parameter is not set, `PatchBundleLoader` will not load the patch.
  Other patches can still use this patch as a dependency
- `description`: A description of the patch. Can be unset if the name is descriptive enough
- `dependencies`: A set of patches which the patch depends on. The patches in this set will be executed before this patch. If a dependency patch raises an exception, this patch will not be executed; subsquently, other patches that depend on this patch will not be executed.
- `compatiblePackages`: A set of `CompatiblePackage` objects. Each `CompatiblePackage` object contains the package name and a set of compatible version names. This parameter can specify the packages and versions the patch is compatible with. Patches can still execute on incompatible packages, but it is recommended to use this parameter to list known compatible packages
  - If unset, it is implied that the patch is compatible with all packages
  - If the set of versions is unset, it is implied that the patch is compatible with all versions of the package
  - If the set of versions is empty, it is implied that the patch is not compatible with any version of the package. This can be useful, for example, to prevent a patch from executing on specific packages that are known to be incompatible

> [!WARNING]
> Circular dependencies are not allowed. If a patch depends on another patch, the other patch cannot depend on the first patch.

> [!NOTE]
> The `@Patch` annotation is optional. If the patch does not require any metadata, it can be omitted.
> If the patch is only used as a dependency, the metadata, such as the `compatiblePackages` parameter, has no effect, as every dependency patch inherits the compatible packages of the patches that depend on it.

> [!TIP]
> An abstract patch class can be annotated with `@Patch`.
> Patches extending off the abstract patch class will inherit the metadata of the abstract patch class.

> [!TIP]
> Instead of the `@Patch` annotation, the superclass's constructor can be used. This is useful in the example scenario where you want to create an abstract patch class.
>
> Example:
>
> ```kt
> abstract class AbstractDisableAdsPatch(
>     fingerprints: Set<Fingerprint>
> ) : BytecodePatch(
>     name = "Disable ads",
>     description = "Disable ads in the app.",
>     fingerprints
> ) {
>   // ...
> }
> ```
>
> Remember that this constructor has precedence over the `@Patch` annotation.

### 🏗️ Patch class

```kt
object DisableAdsPatch : BytecodePatch( /* Parameters */ ) {
  // ...
}
```

Each patch class extends off a base class that implements the `Patch` interface.
The interface requires the `execute` method to be implemented.
Depending on which base class is extended, the patch can modify different parts of the APK as described in [🧩 Introduction to ReVanced Patches](2_introduction_to_patches.md).

> [!TIP]
> A patch is usually a singleton object, meaning only one patch instance exists in the JVM.
> Because dependencies are executed before the patch itself, a patch can rely on the state of the dependency patch.
> This is useful in the example scenario, where the `DisableAdsPatch` depends on the `DisableAdsResourcePatch`.
> The `DisableAdsResourcePatch` can, for example, be used to read the decoded resources of the app and provide the `DisableAdsPatch` with the necessary information to disable ads because the `DisableAdsResourcePatch` is executed before the `DisableAdsPatch` and is a singleton object.

### 🏁 The `execute` function

The `execute` function is declared in the `Patch` interface and needs to be implemented.
The `execute` function receives an instance of a context object that provides access to the APK. The patch can use this context to modify the APK as described in [🧩 Introduction to ReVanced Patches](2_introduction_to_patches.md).

In the current example, the patch adds instructions at the beginning of a method implementation in the Dalvik VM bytecode. The added instructions return `false` to disable ads in the current example:

```kt
val result = LoadAdsFingerprint.result
  ?: throw PatchException("LoadAdsFingerprint not found")

result.mutableMethod.addInstructions(
    0,
    """
        # Return false.
        const/4 v0, 0x0
        return v0
    """
)
```

> [!NOTE]
> This patch uses a fingerprint to find the method and replaces the method's instructions with new instructions.
> The fingerprint is resolved on the classes present in `BytecodeContext`.
> Fingerprints will be explained in more detail on the next page.

> [!TIP]
> The patch can also raise any `Exception` or `Throwable` at any time to indicate that the patch failed to execute. A `PatchException` is recommended to be raised if the patch fails to execute.
> If any patch depends on this patch, the dependent patch will not be executed, whereas other patches that do not depend on this patch can still be executed.
> ReVanced Patcher will handle any exception raised by a patch.

> [!TIP]
> To see real-world examples of patches, check out the [ReVanced Patches](https://github.com/revanced/revanced-patches) repository.

## ⏭️ What's next

The next page explains the concept of fingerprinting in ReVanced Patcher.

Continue: [🔎 Fingerprinting](2_2_1_fingerprinting.md)
