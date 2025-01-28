# 💪 Advanced APIs

A handful of APIs are available to make patch development easier and more efficient.

## 📙 Overview

1. 🔍 Find immutable classes with `classBy(String)`
2. 👹 Create mutable replacements of classes with `mutableClassBy(ClassDef)`
3. 🏃‍ Navigate method calls recursively by index with `navigate(Method)`
4. 💾 Read and write resource files with `get(String, Boolean)` and `delete(String)`
5. 📃 Read and write DOM files using `document(String)` and  `document(InputStream)`

### 🧰 APIs

#### 🔍 `classBy(String)`

The `classBy(String)` function is an alternative to finding immutable classes
from a constant string or from a String field of a fingerprint match. 

```kt
execute {
    // Find the superclass of a fingerprint return type
    val superClassOfReturnType = classBy(match().originalMethod.returnType).superclass
}
```

#### 👹 `mutableClassBy(ClassDef)`

By default, the classes are immutable and they cannot be modified.
To make a class mutable use the `mutableClassBy(ClassDef)` function.
Accessing the property will replace the original class definition with the mutable copy,
thus allowing you to make changes to the class. Subsequent accesses will return the same mutable copy.

```kt
execute {
    // Find a class by the return type of a fingerprint
    val superClassOfReturnType = classBy(match().originalMethod.returnType).superclass

    val mutableClass = mutableClassBy(superClassOfReturnType)
    mutableClass.methods.add(Method())
}
```

#### 🏃‍ `navigate(Method).at(index)`

The `navigate(Method)` function allows navigating method calls by index,
and provides an easier way to parse the method call classes in code. 

```kt
execute {
    // Navigate to the method at index 5 within 'someMethod'.
    // original() returns the original immutable method.
    val original = navigate(someMethod).to(5).original()
    
    // Further navigate to the second occurrence of the opcode 'INVOKE_VIRTUAL'.
    // stop() returns the mutable copy of the method.
    val mutable = navigate(someMethod).to(2) { 
        instruction -> instruction.opcode == Opcode.INVOKE_VIRTUAL
    }.stop()
    
    // You can chain multiple to() calls together navigate multiple calls across different methods and classes.
    //
    // Navigate to:
    // A. the method of the 5th instruction
    // B. the method of the 10th instruction in method A
    // C. the method of 2nd instruction of method B
    val mutableDeep = navigate(someMethod).to(5, 10, 2).stop() // Mutable method Method C
}
```

#### 💾 `get(String, Boolean)` and `delete(String)`

The `get(String, Boolean)` function returns a `File` object that can be used to read and write resource files.

```kt
execute {
    val file = get("res/values/strings.xml")
    val content = file.readText()
    file.writeText(content)
}
```

The `delete` function can mark files for deletion when the APK is rebuilt.

```kt
execute {
    delete("res/values/strings.xml")
}
```

#### 📃 `document(String)`  and  `document(InputStream)`

The `document` function is used to read and write DOM files.

```kt
execute {
    document("res/values/strings.xml").use { document ->
        val element = doc.createElement("string").apply {
            textContent = "Hello, World!"
        }
        document.documentElement.appendChild(element)
    }
}
```

You can also read documents from an `InputStream`:

```kt
execute {
    val inputStream = classLoader.getResourceAsStream("some.xml")
    document(inputStream).use { document ->
        // ...
    }
}
```

## 🎉 Afterword

ReVanced Patcher is a powerful library to patch Android applications, offering a rich set of APIs to develop patches
that outlive app updates. Patches make up ReVanced; without you, the community of patch developers,
ReVanced would not be what it is today. We hope that this documentation has been helpful to you
and are excited to see what you will create with ReVanced Patcher. If you have any questions or need help,
talk to us on one of our platforms linked on [revanced.app](https://revanced.app) or open an issue in case of a bug or
feature request,  
ReVanced
