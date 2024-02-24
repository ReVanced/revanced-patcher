# 🔎 Fingerprinting

In the context of ReVanced, fingerprinting is primarily used to resolve methods with a limited amount of known information.
Methods with obfuscated names that change with each update are primary candidates for fingerprinting.
The goal of fingerprinting is to uniquely identify a method by capturing various attributes of the method, such as the return type, access flags, an opcode pattern, and more.

## ⛳️ Example fingerprint

Throughout the documentation, the following example will be used to demonstrate the concepts of fingerprints:

```kt

package app.revanced.patches.ads.fingerprints

object ShowAdsFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(Opcode.RETURN),
    strings = listOf("pro"),
    customFingerprint = { (methodDef, classDef) -> methodDef.definingClass == "Lcom/some/app/ads/Loader;"}
)
```

## 🔎 Reconstructing the original code from a fingerprint

To understand how a fingerprint is created, the following code is reconstructed from the fingerprint.

The fingerprint contains the following information:

- Package and class name:

  ```kt
  customFingerprint = { it.definingClass == "Lcom/some/app/ads/AdsLoader;"}
  ```

- Method signature:

  ```kt
  returnType = "Z",
  access = AccessFlags.PUBLIC or AccessFlags.FINAL,
  parameters = listOf("Z"),
  ```

- Method implementation:

  ```kt
  strings = listOf("pro"),
  opcodes = listOf(Opcode.RETURN)
  ```

With this information, the original code can be reconstructed:

```java
    package com.some.app.ads;

    <accessFlags> class AdsLoader {
        public final boolean <methodName>(boolean <parameter>) {
            // ...

            var userStatus = "pro";

            // ...

            return <returnValue>;
        }
    }
```

> [!TIP]
> A fingerprint should contain information about a method that is likely to remain the same across updates.
> A name of a method is not included in the fingerprint, because it is likely to change with each update in an obfuscated app, whereas the return type, access flags, parameters, patterns of opcodes and strings are likely to remain the same.

## 🔨 How to use fingerprints

After creating a fingerprint, add it to the constructor of a `BytecodePatch`:

```kt
object DisableAdsPatch : BytecodePatch(
    setOf(ShowAdsFingerprint)
) {
    // ...
 }
```

> [!NOTE]
> Fingerprints passed to the constructor of `BytecodePatch` are resolved by ReVanced Patcher before the patch is executed.

> [!TIP]
> Multiple patches can share fingerprints. If a fingerprint is resolved once, it will not be resolved again.

> [!TIP]
> If a fingerprint has an opcode pattern, you can use the `FuzzyPatternScanMethod` annotation to fuzzy match the pattern.
> Opcode pattern arrays can contain `null` values to indicate that the opcode at the index is unknown.
> Any opcode will match to a `null` value.

> [!WARNING]
> If the fingerprint can not be resolved because it does not match any method, the result of a fingerprint is `null`.

Once the fingerprint is resolved, the result can be used in the patch:

```kt
object DisableAdsPatch : BytecodePatch(
  setOf(ShowAdsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        val result = ShowAdsFingerprint.result
            ?: throw PatchException("ShowAdsFingerprint not found")

        // ...
    }
}
```

The result of a fingerprint that resolved successfully contains mutable and immutable references to the method and the class it is defined in.

```kt
class MethodFingerprintResult(
    val method: Method,
    val classDef: ClassDef,
    val scanResult: MethodFingerprintScanResult,
    // ...
) {
    val mutableClass by lazy { /* ... */ }
    val mutableMethod by lazy { /* ... */ }

    // ...
}

class MethodFingerprintScanResult(
    val patternScanResult: PatternScanResult?,
    val stringsScanResult: StringsScanResult?,
) {
    class StringsScanResult(val matches: List<StringMatch>) {
        class StringMatch(val string: String, val index: Int)
    }

    class PatternScanResult(
        val startIndex: Int,
        val endIndex: Int,
        // ...
    ) {
        // ...
    }
}
```

## 🏹 Manual resolution of fingerprints

Unless a fingerprint is added to the constructor of `BytecodePatch`, the fingerprint will not be resolved automatically by ReVanced Patcher before the patch is executed.
Instead, the fingerprint can be resolved manually using various overloads of the `resolve` method of a fingerprint.

You can resolve a fingerprint in the following ways:

- On a **list of classes**, if the fingerprint can resolve on a known subset of classes

  If you have a known list of classes you know the fingerprint can resolve on, you can resolve the fingerprint on the list of classes:

  ```kt
  override fun execute(context: BytecodeContext) {
      val result = ShowAdsFingerprint.also { it.resolve(context, context.classes) }.result
          ?: throw PatchException("ShowAdsFingerprint not found")

          // ...
  }
  ```

- On a **single class**, if the fingerprint can resolve on a single known class

  If you know the fingerprint can resolve to a method in a specific class, you can resolve the fingerprint on the class:

  ```kt
  override fun execute(context: BytecodeContext) {
      val adsLoaderClass = context.classes.single { it.name == "Lcom/some/app/ads/Loader;" }

      val result = ShowAdsFingerprint.also { it.resolve(context, adsLoaderClass) }.result
          ?: throw PatchException("ShowAdsFingerprint not found")

      // ...
  }
  ```

- On a **single method**, to extract certain information about a method

  The result of a fingerprint contains useful information about the method, such as the start and end index of an opcode pattern or the indices of the instructions with certain string references.
  A fingerprint can be leveraged to extract such information from a method instead of manually figuring it out:

  ```kt
  override fun execute(context: BytecodeContext) {
      val adsFingerprintResult = ShowAdsFingerprint.result
          ?: throw PatchException("ShowAdsFingerprint not found")

      val proStringsFingerprint = object : MethodFingerprint(
          strings = listOf("free", "trial")
      ) {}

      proStringsFingerprint.also {
          it.resolve(context, adsFingerprintResult.method)
      }.result?.let { result ->
          result.scanResult.stringsScanResult!!.matches.forEach { match ->
              println("The index of the string '${match.string}' is ${match.index}")
          }

      } ?: throw PatchException("pro strings fingerprint not found")
  }
  ```

> [!TIP]
> To see real-world examples of fingerprints, check out the [ReVanced Patches](https://github.com/revanced/revanced-patches) repository.

## ⏭️ What's next

The next page discusses the structure and conventions of patches.

Continue: [📜 Project structure and conventions](4_structure_and_conventions.md)
