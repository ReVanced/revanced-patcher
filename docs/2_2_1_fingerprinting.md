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

In the context of ReVanced, fingerprinting is primarily used to match methods with a limited amount of known information.
Methods with obfuscated names that change with each update are primary candidates for fingerprinting.
The goal of fingerprinting is to uniquely identify a method by capturing various attributes, such as the return type,
access flags, an opcode pattern, strings, and more.

## ⛳️ Example fingerprint

Throughout the documentation, the following example will be used to demonstrate the concepts of fingerprints:

```kt

package app.revanced.patches.ads.fingerprints

fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters("Z")
    opcodes(Opcode.RETURN)
    strings("pro")
    custom { (method, classDef) -> method.definingClass == "Lcom/some/app/ads/AdsLoader;" }
}
```

## 🔎 Reconstructing the original code from a fingerprint

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
  custom = { (method, classDef) -> method.definingClass == "Lcom/some/app/ads/AdsLoader;"}
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
> A fingerprint should contain information about a method likely to remain the same across updates.
> A method's name is not included in the fingerprint because it will likely change with each update in an obfuscated app.
> In contrast, the return type, access flags, parameters, patterns of opcodes, and strings are likely to remain the same.

## 🔨 How to use fingerprints

Fingerprints can be added to a patch by directly creating and adding them or by invoking them manually.
Fingerprints added to a patch are matched by ReVanced Patcher before the patch is executed.

```kt
val fingerprint = fingerprint {
    // ...
}

val patch = bytecodePatch {
    // Directly create and add a fingerprint.
    fingerprint {
        // ...
    }

    // Add a fingerprint manually by invoking it.
    fingerprint()
}
```

> [!TIP]
> Multiple patches can share fingerprints. If a fingerprint is matched once, it will not be matched again.

> [!TIP]
> If a fingerprint has an opcode pattern, you can use the `fuzzyPatternScanThreshhold` parameter of the `opcode`
> function to fuzzy match the pattern.  
> `null` can be used as a wildcard to match any opcode:
>
> ```kt
> fingerprint(fuzzyPatternScanThreshhold = 2) {
>    opcodes(
>        Opcode.ICONST_0,
>        null,
>        Opcode.ICONST_1,
>        Opcode.IRETURN,
>    )
>}
> ```

Once the fingerprint is matched, the match can be used in the patch:

```kt
val patch = bytecodePatch {
    // Add a fingerprint and delegate its match to a variable.
    val match by showAdsFingerprint()
    val match2 by fingerprint {
        // ...
    }
  
    execute {
        val method = match.method
        val method2 = match2.method
    }
}
```

> [!WARNING]
> If the fingerprint can not be matched to any method, the match of a fingerprint is `null`. If such a match is delegated
> to a variable, accessing it will raise an exception.

The match of a fingerprint contains mutable and immutable references to the method and the class it matches to.

```kt
class Match(
    val method: Method,
    val classDef: ClassDef,
    val patternMatch: Match.PatternMatch?,
    val stringMatches: List<Match.StringMatch>?,
    // ...
) {
    val mutableClass by lazy { /* ... */ }
    val mutableMethod by lazy { /* ... */ }

    // ...
}
```

## 🏹 Manual matching of fingerprints

Unless a fingerprint is added to a patch, the fingerprint will not be matched automatically by ReVanced Patcher
before the patch is executed.
Instead, the fingerprint can be matched manually using various overloads of a fingerprint's `match` function.

You can match a fingerprint the following ways:

- In a **list of classes**, if the fingerprint can match in a known subset of classes

  If you have a known list of classes you know the fingerprint can match in,
you can match the fingerprint on the list of classes:

  ```kt
    execute { context ->
        val match = showAdsFingerprint.apply { 
            match(context, context.classes) 
        }.match ?: throw PatchException("No match found")
    }
  ```

- In a **single class**, if the fingerprint can match in a single known class

  If you know the fingerprint can match a method in a specific class, you can match the fingerprint in the class:

  ```kt
  execute { context ->
      val adsLoaderClass = context.classes.single { it.name == "Lcom/some/app/ads/Loader;" }

      val match = showAdsFingerprint.apply { 
        match(context, adsLoaderClass)
      }.match ?: throw PatchException("No match found")
  }
  ```

- Match a **single method**, to extract certain information about it

  The match of a fingerprint contains useful information about the method, such as the start and end index of an opcode pattern
or the indices of the instructions with certain string references.
  A fingerprint can be leveraged to extract such information from a method instead of manually figuring it out:

  ```kt
  execute { context ->
      val proStringsFingerprint = fingerprint {
          strings("free", "trial")
      }

      proStringsFingerprint.apply {
          match(context, adsFingerprintMatch.method)
      }.match?.let { match ->
          match.stringMatches.forEach { match ->
              println("The index of the string '${match.string}' is ${match.index}")
          }
      } ?: throw PatchException("No match found")
  }
  ```

> [!TIP]
> To see real-world examples of fingerprints,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## ⏭️ What's next

The next page discusses the structure and conventions of patches.

Continue: [📜 Project structure and conventions](3_structure_and_conventions.md)
