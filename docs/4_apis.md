# 💪 Advanced APIs

A handful of APIs are available to make patch development easier and more efficient.

## 📙 Overview

1. 👹 Mutate classes with `context.proxy(ClassDef)`
2. 🔍 Find and proxy existing classes with `classBy(Predicate)` and `classByType(String)`
3. 🏃‍ Easily access referenced methods recursively by index with `MethodNavigator`
4. 🔨 Make use of extension functions from `BytecodeUtils` and `ResourceUtils` with certain applications
(Available in ReVanced Patches)
5. 💾 Read and write (decoded) resources with `ResourcePatchContext.get(Path, Boolean)`
6. 📃 Read and write DOM files using `ResourcePatchContext.document`

### 🧰 APIs

> [!WARNING]
> This section is still under construction and may be incomplete.

## 🎉 Afterword

ReVanced Patcher is a powerful library to patch Android applications, offering a rich set of APIs to develop patches
that outlive app updates. Patches make up ReVanced; without you, the community of patch developers,
ReVanced would not be what it is today. We hope that this documentation has been helpful to you
and are excited to see what you will create with ReVanced Patcher. If you have any questions or need help,
talk to us on one of our platforms linked on [revanced.app](https://revanced.app) or open an issue in case of a bug or feature request,  
ReVanced
