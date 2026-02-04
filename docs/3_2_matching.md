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

# 🔎 Matching

ReVanced Patcher provides simple APIs to find classes and methods in an app's bytecode.
Methods with obfuscated names that change with each update are primary candidates for this.
The APIs allow capturing unique identifiers of their attributes, such as the return type,
access flags, an opcode pattern, strings, a classes types or fields and more.

## 🆎 Variants of the API

The APIs are available in multiple variants, which build on each other. The APIs start with `first`, 
have `immutable`, `orNull`, delegate, extension, predicate and declarative variants.
Depending on the use-case, a certain variant is more suitable.

```kt
bytecodePatch {
    apply { 
        // Find the first class matching the predicate, or raises an exception if no match is found: 
        firstClassDef { type == "Lcom/some/Class;" } 
      
        // Returns null if no match is found:
        assertIsNull(firstClassDefOrNull { type == "No class has such a type" })
        
        // Declarative variant:
        firstClassDefDeclaratively {
            predicate { type == "Lcom/some/Class;" }
        }

        // Declarative variant, returns null if no match is found:
        assertIsNull(firstClassDefDeclarativelyOrNull {
          predicate { type == "No class has such a type" }
        })
      
        // Find the first method matching the predicate, or raises an exception if no match is found:
        firstMethod { name == "someMethodName" && definingClass == "Lcom/some/Class;" }
      
        // Returns null if no match is found:
        assertIsNull(firstMethodOrNull { name == "No method has such a name" })
      
        // Declarative variant:
        firstMethodDeclaratively {
          predicate { name == "someMethodName" }
          predicate { definingClass == "Lcom/some/Class;" }
        }

        // Declarative variant, returns null if no match is found:
        assertIsNull(firstMethodDeclarativelyOrNull {
          predicate { name == "No method has such a name" }
        })
    }
}
```

For the declarative API `anyOf`, `allOf` and `noneOf` blocks are also available to build predicate logic declaratively:

```kt
firstMethodDeclaratively {
    anyOf {
        predicate { name == "someMethodName" }
        predicate { name == "anotherMethodName" }
    }
    noneOf {
        allOf {
          predicate { definingClass == "Lcom/some/Class;" }
          predicate { accessFlags.contains(AccessFlags.PUBLIC) }
        }
        predicate { returnType == "V" }
    }
}
```

These APIs are extension functions of `BytecodePatchContext` and match in `BytecodePatchContext.classDefs`.
To match in a custom list of classes, a single class, or a list of methods, extension variants
of these APIs are available. The usage is identical.
A specific variant are the delegate functions, prefixed by `getting`.
These allow using the APIs outside the `apply` block of a patch.

```kt
val BytecodePatchContext.someMethod by gettingFirstMethod {
  name == "someMethodName" && definingClass == "Lcom/some/Class;"
}

bytecodePatch {
    apply { 
        // Use the delegated property to access the matched method:
        someMethod.name
    }
}
```

By default, these APIs return mutable copies of the matched class or method.
If only read-only access is needed, immutable variants of these APIs should be used, prefixed by `immutable`.

```kt
firstImmutableClassDef { type == "Lcom/some/Class;" }
```

## 🎨 Extensions

To make matching more expressive, multiple extension functions are available to match
by specific attributes of a method or class. Some examples are shown below:

```kt
firstMethod {
    implementation { anyInstruction { opcode == Opcode.RETURN } } && anyParameter { type == "I" }
}

firstClassDef {
    anyField { accessFlags.all { it == AccessFlags.PRIVATE } && type == "Ljava/lang/String;" } &&
            anyMethod { name == "toString"  }
}
```

Specifically for the declarative variants, notable extension functions are available to match
by common attributes of a method, which also work in combination with `anyOf`, `allOf` and `noneOf` blocks:

```kt
firstMethodDeclaratively {
    anyOf {
        name("someMethodName")
        accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
    }
    noneOf {
        definingClass("Lcom/some/Class;")
        allOf {
            accessFlags(AccessFlags.PUBLIC, AccessFlags.STATIC)
            returnType("V")  
        }
    }
    parameterTypes("I", "Ljava/lang/String;")
    predicate { parameterTypes.count() == 2 }
    custom {parameterTypes.count() == 2 } // Same as predicate.
}
```

Some of the APIs also have variants such as predicates:

```kt
firstMethodDeclaratively  {
    name { startsWith("some") }
    definingClass { startsWith("Lcom/some/") }
    returnType { it == "V" || it == "Z" }
}
```

## 📃 Matcher API

The matcher API is a subset of the matching API
that allows creating reusable matchers for lists using the `Matcher<T, U>` class.

A specific implementation of this class is the `IndexedMatcher<T>` class,
which allows matching items in a list by their index.

```kt
val match = indexedMatcher<Int>(
    { lastMatchedIndex, currentIndex, setNextIndex ->
        currentIndex == 0 && this == 1
    },
    { _,_,_ -> this == 2 },
    { _,_,_ -> this == 3 },
)

assertFalse(match(listOf(0, 1, 0, 0, 2, 0, 3)))
assertEmpty(match.indices)

assertTrue(match(listOf(1, 0, 0, 2, 0, 3)))
assertEquals(match.indices, listOf(0, 3, 5))
```

Special functions also exist such as:

```kt
indexedMatcher<Int>(
    at(0) { this == 1 },
    at(3) { this == 2 },
    after(1..2) { this == 3 },
    after { this == 2 },
    afterAtMin(2) { this == 3 },
    afterAtMost(2) { this == 3 },
)
```

A specific use-case of the indexed matcher is to match instructions in a method:

```kt
val match = indexedMatcher<Instruction>(
    { _, _, _ -> opcode == Opcode.CONST_STRING },
    { _, _, _ -> (this as? ReferenceInstruction)?.reference?.toString()?.contains("myMethod") == true }
)
```

For this purpose, once again functions are available to simplify the usage of the indexed matcher for instructions.
Some notable examples are shown below:

```kt
indexedMatcher(
    opcode(Opcode.CONST_STRING),
    type("Lcom/some/Class;"),
    method("someMethodName"),
    field("someFieldName"),
    string("someString"),
    reference("Lcom/some/Class;->someMethod()V"),
    literal(1L),
    registers(1, 2, 3),
    instruction { opcode == Opcode.INVOKEVIRTUAL },
    allOf(noneOf(opcode(Opcode.CONST_STRING), opcode(Opcode.CONST)), anyOf(string("A"), string("B")))
)
```

These APIs have lambda as well as invoke variants as well for convenience:

```kt
indexedMatcher(
    Opcode.CONST_STRING(),
    "string" { contains(it) },
    "string"(String::contains),
    1L(),
    method { definingClass == "Lcom/some/Class;" && name == "someMethodName" },
    reference { it is StringReference },
)
```

Wildcards can be used to match any instruction, string, method, field or reference:

```kt
indexedMatcher(
    string(), // Matches any string.
    method(), // Matches any method.
    field(), // Matches any field.
    reference(), // Matches any reference.
    noneOf(), // Matches any instruction, because the noneOf block is empty.
)
```

Of course all APIs can be used interchangeably:

```kt
indexedMatcher(
    at(2, afterAtMost(3, allOf(Opcode.CONST_STRING(), "str"(String::startsWith))))
)
```

A helper function exists to create unordered matching predicates that can be used with an indexed matcher:

```kt
indexedMatcher(predicates = unorderedAllOf("string"(), "string2"(String::contains)))
```

An extension function exists to simplify the usage of the indexed matcher:

```kt
val matched = listOf<Instruction>().matchIndexed("string"(), method())
```

When used with declarative variants of the matching API,
the following APIs are available to use indexed matchers in a declarative way:

```kt
firstMethodDeclaratively {
    instructions(
        string(),
        method()
    )
    opcodes(Opcode.CONST_STRING, Opcodes.INVOKE_VIRTUAL) // Matches sequentially using after().
    strings("A", "B", "C") // Matches using unorderedAllOf().
}
```

## The composite API

The composite API combines the matching and matcher APIs 
targeting the common use-case of matching methods and finding indices of instructions in a method.
Once again, delegate and extension variants of the composite API are available to
match in a custom list of classes, a single class or a list of methods, parallel to the variants of the matching API.

```kt
val match = firstMethodComposite {
    name("someMethodName")
    instructions(
        at(0) { opcode == Opcode.CONST_STRING },
        after(2..3, method("name"))
    )
    strings("someString", "anotherString")
    opcodes(Opcode.INVOKEVIRTUAL, Opcode.INVOKEINTERFACE)
}

match.method
match.methodOrNull
match.immutableMethod
match.immutableMethodOrNull
match.classDef
match.classDefOrNull
match.immutableClassDef
match.immutableClassDefOrNull

// Get the indices of the matchers:
val instructionIndices = match.indices[0]
val stringIndices = match.indices[1]
val opcodeIndices = match.indices[2]
```

For convenience, the `Match` class overloads the following operators:

```kt
val match = firstMethodComposite {
    // ...
}

match[0] // Shorthand for match.indices[0].
match[0, 0] // Shorthand for match.indices[0][0].
match[-1, -1] // Also supports negative indices, shorthand for match.indices[match.indices.size - 1][match.indices[0].size - 1].

val (method, indices, immutableClassDef) = match // "indices" is match.indices[0].
```

## ⛳️ Example usage

```kt
package app.revanced.patches.ads.com.some.app

val BytecodePatchContext.loadAdsMethod by composingFirstMethod {
    definingClass("Lcom/some/app/ads/Loader;")
    accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
    returnType("Z")
    parameterTypes("Z")
    opcodes(Opcode.RETURN)
    strings("pro")
}
```

## 🔎 Reconstructing the original code from the example above

The following information can be extracted from the example above:

- Method signature:

  ```kt
  accessFlags(AccessFlags.PUBLIC, AccessFlags.FINAL)
  returnType("Z")
  parameterTypes("Z")
  ```

- Method implementation:

  ```kt
  opcodes(Opcode.RETURN)
  strings("pro")
  ```

- Package and class name:

  ```kt
  definingClass("Lcom/some/app/ads/AdsLoader;")
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

With the attributes specified, this method can be matched uniquely from all other methods.

> [!TIP]
> Rely on attributes likely to remain the same across updates.
> A method's name is not checked because it will likely change with each update in an obfuscated
> app.
> In contrast, the return type, access flags, parameters, patterns of opcodes, and strings are likely to remain the
> same.

> [!TIP]
> To see real-world examples of fingerprints,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## ⏭️ What's next

The next page discusses the structure and conventions of patches.

Continue: [📜 Project structure and conventions](4_structure_and_conventions.md)
