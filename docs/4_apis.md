# 💪 Advanced APIs

A handful of APIs are available to make patch development easier and more efficient.

## 📙 Overview

1. 👹 Create mutable replacements of classes with `proxy(ClassDef)`
2. 🔍 Find and create mutable replaces with `classBy(Predicate)`
3. 🏃‍ Navigate method calls recursively by index with `navigate(Method)`
4. 💾 Read and write resource files with `get(String, Boolean)` and `delete(String)`
5. 📃 Read and write DOM files using `document(String)` and  `document(InputStream)`

### 🧰 APIs

#### 👹 `proxy(ClassDef)`

By default, the classes are immutable, meaning they cannot be modified.
To make a class mutable, use the `proxy(ClassDef)` function.
This function creates a lazy mutable copy of the class definition.
Accessing the property will replace the original class definition with the mutable copy,
thus allowing you to make changes to the class. Subsequent accesses will return the same mutable copy.

```kt
execute {
    val mutableClass = proxy(classDef)
    mutableClass.methods.add(Method())
}
```

#### 🔍 `classBy(Predicate)`

The `classBy(Predicate)` function is an alternative to finding and creating mutable classes by a predicate.
It automatically proxies the class definition, making it mutable.

```kt
execute {
    // Alternative to proxy(classes.find { it.name == "Lcom/example/MyClass;" })?.classDef
    val classDef = classBy { it.name == "Lcom/example/MyClass;" }?.classDef
}
```

#### 🏃‍ `navigate(Method).at(index)`

The `navigate(Method)` function allows you to navigate method calls recursively by index.

```kt
execute {
    // Sequentially navigate to the instructions at index 1 within 'someMethod'.
    val method = navigate(someMethod).to(1).original() // original() returns the original immutable method.
    
    // Further navigate to the second occurrence where the instruction's opcode is 'INVOKEVIRTUAL'.
    // stop() returns the mutable copy of the method.
    val method = navigate(someMethod).to(2) { instruction -> instruction.opcode == Opcode.INVOKEVIRTUAL }.stop()
    
    // Alternatively, to stop(), you can delegate the method to a variable.
    val method by navigate(someMethod).to(1)
    
    // You can chain multiple calls to at() to navigate deeper into the method.
    val method by navigate(someMethod).to(1).to(2, 3, 4).to(5)
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
