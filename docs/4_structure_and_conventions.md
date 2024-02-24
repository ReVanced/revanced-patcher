# 📜 Project structure and conventions

Over time, a specific project structure and conventions have been established.

## 📁 File structure

Patches are organized in a specific file structure. The file structure is as follows:

```text
📦your.patches.app.category
 ├ 📂fingerprints
 ├ ├ 🔍SomeFingerprintA.kt
 ├ └ 🔍SomeFingerprintB.kt
 └ 🧩SomePatch.kt
```

## 📙 Conventions

- 🔥 Name a patch after what it does. For example, if a patch removes ads, name it `RemoveAdsPatch`.
  If a patch changes the color of a button, name it `ChangeButtonColorPatch`
- 🔥 Write the patch description in the third person, present tense, and end it with a period.
  If a patch removes ads, the description can be omitted because of redundancy, but if a patch changes the color of a button, the description can be _Changes the color of the resume button to red._
- 🔥 Write patches with modularity and reusability in mind. Patches can depend on each other, so it is important to write patches in a way that can be used in different contexts.
- 🔥🔥 Keep patches as minimal as possible. This reduces the risk of failing patches.
  Instead of involving many abstract changes in one patch or writing entire methods or classes in a patch,
  you can write code in integrations. Integrations are compiled classes that are merged into the app before patches are executed as described in [💉 Introduction to ReVanced Patcher](1_introduction_to_patcher.md).
  Patches can then reference methods and classes from integrations.
  A real-world example of integrations can be found in the [ReVanced Integrations](https://github.com/ReVanced/revanced-integrations) repository
- 🔥🔥🔥 Do not overload a fingerprint with information about a method that's likely to change.
  In the example of an obfuscated method, it's better to fingerprint the method by its return type and parameters rather than its name because the name is likely to change. An intelligent selection of an opcode pattern or strings in a method can result in a strong fingerprint dynamic to app updates.
- 🔥🔥🔥 Document your patches. Patches are abstract by nature, so it is important to document parts of the code that are not self-explanatory. For example, explain why and how a certain method is patched or large blocks of instructions that are modified or added to a method

## ⏭️ What's next

The next page discusses useful APIs for patch development.

Continue: [💪 Advanced APIs](5_apis.md)
