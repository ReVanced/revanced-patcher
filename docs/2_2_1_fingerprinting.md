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
access flags, instructions, strings, and more.

## 🔎 Example target Java code and bytecode

```java
package com.some.app.ads;

class AdsLoader {
  private final static Map<String, String> a = new HashMap<>();

  // Method to fingerprint    
  public final boolean obfuscatedMethod(String parameter1, int parameter2) {
    // Filter 1 target instruction.
    String value1 = a.get(parameter1);

    unrelatedMethod(value1);

    // Filter 2, 3, 4 target instructions, and the instructions to modify.
    if ("showBannerAds".equals(value1)) {
      showBannerAds();
    }

    // Filter 4 target instruction.
    return parameter2 != 1337;
  }

  private void showBannerAds() {
    // ...
  }

  private void unrelatedMethod(String parameter) {
    // ...
  }
}
```

```asm
# Method to fingerprint
.method public final obfuscatedMethod(Ljava/lang/String;I)Z
    .registers 4

    # Filter 1 target instruction.
    sget-object v0, Lapp/revanced/extension/shared/AdsLoader;->a:Ljava/util/Map;

    invoke-interface {v0, p1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Ljava/lang/String;

    invoke-direct {p0, p1}, Lapp/revanced/extension/shared/AdsLoader;->unrelatedMethod(Ljava/lang/String;)V

    # Filter 2 target instruction.
    const-string v0, "showBannerAds"

    # Filter 3 target instruction.
    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    # Filter 4 target instruction.
    move-result p1

    if-eqz p1, :cond_16

    invoke-direct {p0}, Lapp/revanced/extension/shared/AdsLoader;->showBannerAds()V

    # Filter 5 target instruction.
    :cond_16
    const/16 p1, 0x539

    if-eq p2, p1, :cond_1c

    const/4 p1, 0x1

    goto :goto_1d

    :cond_1c
    const/4 p1, 0x0

    :goto_1d
    return p1
.end method
```

## ⛳️ Example fingerprint

```kt
package app.revanced.patches.ads.fingerprints

val hideAdsFingerprint by fingerprint { 
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returns("Z")
    parameters("Ljava/lang/String;", "I")
    instructions( 
        // Filter 1
        fieldAccess(
            definingClass = "this",
            type = "Ljava/util/Map;"
        ),

        // Filter 2 
        string("showBannerAds"),
      
        // Filter 3 
        methodCall(
            definingClass = "Ljava/lang/String;",
            name = "equals",
        ),

        // Filter 4
        opcode(Opcode.MOVE_RESULT),

        // Filter 5
        literal(1337)
    )
    custom { method, classDef ->
        classDef.type == "Lapp/revanced/extension/shared/AdsLoader;"
    }
}
```

The fingerprint contains the following information:

- Method signature:

  ```kt
  accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
  returns("Z")
  parameters("Ljava/lang/String;", "I")
  ```

- Method implementation:

  ```kt
  instructions( 
      // Filter 1
      fieldAccess(
          definingClass = "this",
          type = "Ljava/util/Map;"
      ),

      // Filter 2 
      string("showBannerAds"),
  
      // Filter 3
      methodCall(
          definingClass = "Ljava/lang/String;",
          name = "equals",
      ),

      // Filter 4
      opcode(Opcode.MOVE_RESULT),

      // Filter 5
      literal(1337)
  )
```

  Notice the instruction filters do not declare every instruction in the target method,
  and between each filter can exist 0 or more other instructions.  Instruction filters
  must be declared in the same order the instructions appear in the target method.

  If a method cannot be uniquely identified using the built in filters, but a fixed
  pattern of opcodes can identify the method, then the opcode pattern can be
  defined using the fingerprint `opcodes { }` declaration.  Opcode patterns do not
  allow variable spacing between each opcode, and all opcodes all must appear exactly as declared.
  In general opcode patterns should be avoided due to their fragility.

- Package and class name:

  ```kt
  custom { (method, classDef) -> classDef == "Lcom/some/app/ads/AdsLoader;" }
  ```

With this information, the original code can be reconstructed:


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
execute {
  hideAdsFingerprint.let {
    val filter4 = it.instructionMatches[3]

    val moveResultIndex = filter3.index
    val moveResultRegister = filter3.getInstruction<OneRegisterInstruction>().registerA

    // Changes the target code to:
    // if (false) {
    //    showBannerAds();
    // } 
    it.method.addInstructions(moveResultIndex + 1, "const/4 v$moveResultRegister, 0x0")
  }
}
```

For performance reasons, a fingerprint will always match only once.
This makes it useful to share fingerprints between multiple patches,
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
    val match = showAdsFingerprint.match(classes)
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
    val match = showAdsFingerprint.match(adsLoaderClassFingerprint.originalClassDef)
  }
  ```

- Match a **single method**, to extract certain information about it

  The match of a fingerprint contains useful information about the method,
  such as the start and end index of an instruction filters or the indices of the instructions with certain string
  references.
  A fingerprint can be leveraged to extract such information from a method instead of manually figuring it out:

  ```kt
  execute {
    val currentPlanFingerprint = fingerprint {
        instructions(
            // literal strings, in the same order they appear in the target method. 
            string("showads"),
            string("userid")
        ) 
    }

    currentPlanFingerprint.match(adsFingerprint.method).let { match ->
      match.instructionMatches.forEach { match ->
        println("The index of the string is ${match.index}")
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
