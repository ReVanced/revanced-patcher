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

# 🔎 Fingerprinting

In the context of ReVanced, a fingerprint is a partial description of a method.
It is used to uniquely match a method by its characteristics.
Fingerprinting is used to match methods with a limited amount of known information.
Methods with obfuscated names that change with each update are primary candidates for fingerprinting.
The goal of fingerprinting is to uniquely identify a method by capturing various attributes, such as the return type,
access flags, an opcode pattern, strings, and more.

## ⛳️ Example fingerprint

An example fingerprint is shown below:

```kt

package app.revanced.patches.ads.fingerprints

fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters("Z")
    opcodes(Opcode.RETURN)
    strings("pro")
    custom { (method, classDef) -> classDef == "Lcom/some/app/ads/AdsLoader;" }
}
```

## 🔎 Reconstructing the original code from the example fingerprint from above

The following code is reconstructed from the fingerprint to understand how a fingerprint is created.

The fingerprint contains the following information:

- Method signature:

  ```kt
  accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
  returns("Z")
  parameters("Z")
  ```

- Method implementation:

  ```kt
  opcodes(Opcode.RETURN)
  strings("pro")
  ```

- Package and class name:

  ```kt
  custom { (method, classDef) -> classDef == "Lcom/some/app/ads/AdsLoader;" }
  ```

With this information, the original code can be reconstructed:

```java
package com.some.app.ads;

<accessFlags>

class AdsLoader {
    public final boolean <methodName>(boolean <parameter>)

    {
        // ...

        var userStatus = "pro";

        // ...

        return <returnValue >;
    }
}
```

Using that fingerprint, this method can be matched uniquely from all other methods.

> [!TIP]
> A fingerprint should contain information about a method likely to remain the same across updates.
> A method's name is not included in the fingerprint because it will likely change with each update in an obfuscated
> app.
> In contrast, the return type, access flags, parameters, patterns of opcodes, and strings are likely to remain the
> same.

## 🔨 How to use fingerprints

After declaring a fingerprint, it can be used in a patch to find the method it matches to:

```kt
val fingerprint = fingerprint {
    // ...
}

val patch = bytecodePatch {
    execute {
        fingerprint.method
    }
}
```

The fingerprint won't be matched again, if it has already been matched once, for performance reasons.
This makes it useful, to share fingerprints between multiple patches,
and let the first executing patch match the fingerprint:

```kt
// Either of these two patches will match the fingerprint first and the other patch can reuse the match:
val mainActivityPatch1 = bytecodePatch {
    execute {
        mainActivityOnCreateFingerprint.method
    }
}

val mainActivityPatch2 = bytecodePatch {
    execute {
        mainActivityOnCreateFingerprint.method
    }
}
```

> [!WARNING]
> If the fingerprint can not be matched to any method,
> accessing certain properties of the fingerprint will raise an exception.
> Instead, the `orNull` properties can be used to return `null` if no match is found.

> [!TIP]
> If a fingerprint has an opcode pattern, you can use the `fuzzyPatternScanThreshhold` parameter of the `opcode`
> function to fuzzy match the pattern.  
> `null` can be used as a wildcard to match any opcode:
>
> ```kt
> fingerprint(fuzzyPatternScanThreshhold = 2) {
>   opcodes(
>     Opcode.ICONST_0,
>     null,
>     Opcode.ICONST_1,
>     Opcode.IRETURN,
>    )
>}
> ```

The following properties can be accessed in a fingerprint:

- `originalClassDef`: The original class definition the fingerprint matches to.
- `originalClassDefOrNull`: The original class definition the fingerprint matches to.
- `originalMethod`: The original method the fingerprint matches to.
- `originalMethodOrNull`: The original method the fingerprint matches to.
- `classDef`: The class the fingerprint matches to.
- `classDefOrNull`: The class the fingerprint matches to.
- `method`: The method the fingerprint matches to. If no match is found, an exception is raised.
- `methodOrNull`: The method the fingerprint matches to.

The difference between the `original` and non-`original` properties is that the `original` properties return the
original class or method definition, while the non-`original` properties return a mutable copy of the class or method.
The mutable copies can be modified. They are lazy properties, so they are only computed
and only then will effectively replace the `original` method or class definition when accessed.

> [!TIP]
> If only read-only access to the class or method is needed,
> the `originalClassDef` and `originalMethod` properties should be used,
> to avoid making a mutable copy of the class or method.

## 🏹 Manually matching fingerprints

By default, a fingerprint is matched automatically against all classes
when one of the fingerprint's properties is accessed.

Instead, the fingerprint can be matched manually using various overloads of a fingerprint's `match` function:

- In a **list of classes**, if the fingerprint can match in a known subset of classes

  If you have a known list of classes you know the fingerprint can match in,
  you can match the fingerprint on the list of classes:

  ```kt
  execute {
    val match = showAdsFingerprint(classes)
  }
  ```

- In a **single class**, if the fingerprint can match in a single known class

  If you know the fingerprint can match a method in a specific class, you can match the fingerprint in the class:

  ```kt
  execute {
    val adsLoaderClass = classes.single { it.name == "Lcom/some/app/ads/Loader;" }

    val match = showAdsFingerprint.match(adsLoaderClass)
  }
  ```

  Another common usecase is to use a fingerprint to reduce the search space of a method to a single class.

  ```kt
  execute {
    // Match showAdsFingerprint in the class of the ads loader found by adsLoaderClassFingerprint.
    val match = showAdsFingerprint.match(adsLoaderClassFingerprint.classDef)
  }
  ```

- Match a **single method**, to extract certain information about it

  The match of a fingerprint contains useful information about the method,
  such as the start and end index of an opcode pattern or the indices of the instructions with certain string
  references.
  A fingerprint can be leveraged to extract such information from a method instead of manually figuring it out:

  ```kt
  execute {
    val currentPlanFingerprint = fingerprint {
      strings("free", "trial")
    }

    currentPlanFingerprint.match(adsFingerprint.method).let { match ->
      match.stringMatches.forEach { match ->
        println("The index of the string '${match.string}' is ${match.index}")
      }
    }
  }
  ```

> [!WARNING]
> If the fingerprint can not be matched to any method, calling `match` will raise an
> exception.
> Instead, the `orNull` overloads can be used to return `null` if no match is found.

> [!TIP]
> To see real-world examples of fingerprints,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## ⏭️ What's next

The next page discusses the structure and conventions of patches.

Continue: [📜 Project structure and conventions](3_structure_and_conventions.md)
