<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="assets/revanced-headline/revanced-headline-vertical-dark.svg"
    >
    <img 
      width="256px"
      src="assets/revanced-headline/revanced-headline-vertical-light.svg"
    >
  </picture>
  <br>
  <a href="https://revanced.app/">
     <picture>
         <source height="24px" media="(prefers-color-scheme: dark)" srcset="assets/revanced-logo/revanced-logo.svg" />
         <img height="24px" src="assets/revanced-logo/revanced-logo.svg" />
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

# 💉 ReVanced Patcher

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/ReVanced/revanced-patcher/release.yml)
![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

ReVanced Patcher used to patch Android applications.

## ❓ About

ReVanced Patcher is a library that is used to patch Android applications.  
It powers [ReVanced Manager](https://github.com/ReVanced/revanced-manager),
[ReVanced CLI](https://github.com/ReVanced/revanced-cli)
and [ReVanced Library](https://github.com/ReVanced/revanced-library) and a rich set of patches have been developed
using ReVanced Patcher in the [ReVanced Patches](https://github.com/ReVanced/revanced-patches) repository.

## 💪 Features

Some of the features the ReVanced Patcher provides are:

- 🔧 **Patch Dalvik VM bytecode**: Disassemble and assemble Dalvik bytecode
- 📦 **Patch APK resources**: Decode and build Android APK resources
- 📂 **Patch arbitrary APK files**: Read and write arbitrary files directly from and to APK files
- 🧩 **Write modular patches**: Extensive API to write modular patches that can patch Dalvik VM bytecode,
APK resources and arbitrary APK files

## 🚀 How to get started

To use ReVanced Patcher in your project, follow these steps:

1. [Add the repository](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
to your project
2. Add the dependency to your project:

   ```kt
    dependencies {
        implementation("app.revanced:revanced-patcher:{$version}")
    }
   ```

For a minimal project configuration, 
see [ReVanced Patches template](https://github.com/ReVanced/revanced-patches-template).

## 📚 Everything else

### 📙 Contributing

Thank you for considering contributing to ReVanced Patcher.
You can find the contribution guidelines [here](CONTRIBUTING.md).

### 🛠️ Building

To build ReVanced Patcher,
you can follow the [ReVanced documentation](https://github.com/ReVanced/revanced-documentation).

### 📃 Documentation

The documentation contains the fundamentals of ReVanced Patcher and how to use ReVanced Patcher to create patches.
You can find it [here](https://github.com/ReVanced/revanced-patcher/tree/main/docs).

## 📜 Licence

ReVanced Patcher is licensed under the GPLv3 license. Please see the [licence file](LICENSE) for more information.
[tl;dr](https://www.tldrlegal.com/license/gnu-general-public-license-v3-gpl-3) you may copy, distribute and modify ReVanced Patcher as long as you track changes/dates in source files.
Any modifications to ReVanced Patcher must also be made available under the GPL,
along with build & install instructions.
