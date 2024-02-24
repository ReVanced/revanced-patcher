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

1.  📝 Patch annotation

    ```kt
    @Patch(
        name = "Disable ads",
        description = "Disable ads in the app.",
        dependencies = [DisableAdsResourcePatch::class],
        compatiblePackages = [CompatiblePackage("com.some.app", ["1.3.0"])]
    )
    ```

    The `@Patch` annotation is used to provide metadata about the patch.

    Noteable annotation parameters are:

    - `name`: The name of the patch. This is used as an identifier for the patch.
      If this parameter is not set, `PatchBundleLoader` will not load the patch.
      Other patches can still use this patch as a dependency
    - `description`: A description of the patch. Can be unset if the name is descriptive enough
    - `dependencies`: A set of patches which the patch depends on. The patches in this set will be executed before this patch. If a dependency patch raises an exception, this patch will not be executed, subsquently, other patches that depend on this patch will not be executed.
    - `compatiblePackages`: A set of `CompatiblePackage` objects. Each `CompatiblePackage` object contains the package name and a set of compatible version names. This parameter can be used to specify the packages and versions the patch is compatible with. Patches can still execute on incompatible packages, but it is recommended to use this parameter to list known compatible packages
      - If unset, it is implied that the patch is compatible with all packages
      - If the set of versions is unset, it is implied that the patch is compatible with all versions of the package
      - If the set of versions is empty, it is implied that the patch is not compatible with any version of the package. This can be useful, for example, to prevent a patch from executing on specific packages that are known to be incompatible

    > [!WARNING]
    > Circular dependencies are not allowed. If a patch depends on another patch, the other patch cannot depend on the first patch.

    > [!NOTE]
    > The `@Patch` annotation is optional. If the patch does not require any metadata, it can be omitted.
    > If the patch is only used as a dependency, the metadata such as the `compatiblePackages` parameter has no effect, as every dependency patch inherits the compatible packages of the patches that depend on it.

    > [!TIP]
    > An abstract patch class can be annotated with `@Patch`.
    > Patches extending off the abstract patch class will inherit the metadata of the abstract patch class.

    > [!TIP]
    > Instead of the `@Patch` annotation, the constructor of the super class can be used. This is useful in the example scenario where you want to create an abstract patch class.
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
    > Keep in mind, that this constructor has precedence over the `@Patch` annotation.

2.  🏗️ Patch class

    ```kt
    object DisableAdsPatch : BytecodePatch( /* Parameters */ ) {
      // ...
    }
    ```

    Each patch class extends off a base class that implements the `Patch` interface.
    The interface requires the `execute` method to be implemented.
    Depending on which base class is extended, the patch can modify different parts of the APK as described in [🧩 Introduction to ReVanced Patches](2_introduction_to_patches.md).

    > [!TIP]
    > A patch is usually a singleton object, meaning that there is only one instance of the patch in the JVM.
    > Because dependencies are executed before the patch itself, a patch can rely on the state of the dependency patch.
    > This is useful in the example scenario, where the `DisableAdsPatch` depends on the `DisableAdsResourcePatch`.
    > The `DisableAdsResourcePatch` can for example be used to read the decoded resources of the app and provide the `DisableAdsPatch` with the necessary information to disable ads, because the `DisableAdsResourcePatch` is executed before the `DisableAdsPatch` and is a singleton object.

3.  🏁 The `execute` function

    The `execute` function is declared in the `Patch` interface and, therefore required to be implemented.
    The `execute` function receives an instance of a context object that provides access to the APK. The patch can use this context to modify the APK as descriped in [🧩 Introduction to ReVanced Patches](2_introduction_to_patches.md).

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
    > This patch uses a fingerprint to find the method and replaces the instructions of the method with new instructions.
    > The fingerprint is resolved on the classes present in `BytecodeContext` and the method is resolved on the class that the fingerprint is found in.
    > Fingerprints will be explained in more detail on the next page.

    > [!TIP]
    > The patch can also raise any `Exception` or `Throwable` at any time to indicate that the patch failed to execute. A `PatchException` is recommended to be raised if the patch fails to execute.
    > If any patch depends on this patch, the dependent patch will not be executed, whereas other patches that do not depend on this patch can still be executed.
    > Any exception raised by a patch will be handled by ReVanced Patcher.

> [!TIP]
> To see real-world examples of fingerprints, check out the [ReVanced Patches](https://github.com/revanced/revanced-patches) repository.

## ⏭️ What's next

The next page explains the concept of fingerprinting in ReVanced Patcher.

Continue: [🔎 Fingerprinting](3_fingerprinting.md)
