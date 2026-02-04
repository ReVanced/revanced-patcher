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

# 🧩 Introduction to ReVanced Patches

ReVanced Patches is a subset of the ReVanced Patcher API
providing a modular way to modify specific parts of an Android application.

## 📙 Fundamentals

There are three kinds of patches. Each kind can modify a different part of the APK, such as the Dalvik VM bytecode, 
the APK resources, or arbitrary files in the APK:

- A `BytecodePatch` focuses on Dalvik VM bytecode
- A `ResourcePatch` focuses on (decoded) resources
- A `RawResourcePatch` focuses on arbitrary files inside an APK

Each patch can declare a set of dependencies on other patches. ReVanced Patcher will first apply dependencies
before applying the patch itself. This way, multiple patches can work together for abstract purposes in a modular way.

The `apply` function is the entry point for a patch. It is called by ReVanced Patcher when the patch is applied.
The `apply` function receives an instance of a context object that provides access to the APK
corresponding to the kind of patch. The patch can use this context to modify the APK.

Each type of context provides different APIs to modify the APK. For example, `BytecodePatchContext` provides APIs
to access Dalvik VM bytecode, while `ResourcePatchContext` provides APIs to access resources.

The subtle difference between `ResourcePatch` and `RawResourcePatch` is that ReVanced Patcher decodes
all APK resources for a `ResourcePatch` when it is executed but not for `RawResourcePatch`.
Both, `ResourcePatch` and `RawResourcePatch` can modify arbitrary files in the APK.
The choice of which kind of patch to use depends on the use case.
Decoding and building resources is a time- and resource-consuming,
so if the patch does not need to modify decoded resources, it is better to use `RawResourcePatch` or `BytecodePatch`.

Example of patches:

```kt
@Surpress("unused")
val bytecodePatch = bytecodePatch(name = "Bytecode patch") {
    apply { 
        // More about this on the next page of the documentation.
    }
}

@Surpress("unused")
val rawResourcePatch = rawResourcePatch(name = "Raw resource patch") {
    apply {
        // More about this on the next page of the documentation.
    }
}

@Surpress("unused")
val resourcePatch = resourcePatch(name = "Resource patch") {
    apply {
        // More about this on the next page of the documentation.
    }
}
```

In modern versions of Android, fields can have whitespace.
The `creating` variants for these APIs allow you to omit the name and infer it from the variable name:

```kt
val `Some patch` by creatingBytecodePatch {
    apply {
        // More about this on the next page of the documentation.
    }
}
```

> [!TIP]
> To see real-world examples of patches,
> check out the repository for [ReVanced Patches](https://github.com/revanced/revanced-patches).

## ⏭️ Whats next

The next page shows how a patch is used based on an example.

Continue: [🧩 Anatomy of a ReVanced patch](3_1_patch_anatomy.md)
