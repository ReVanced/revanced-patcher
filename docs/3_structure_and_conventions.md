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

# 📜 Project structure and conventions

Over time, a specific project structure and conventions have been established.

## 📁 File structure

Patches are organized in a specific way. The file structure looks as follows:

```text
📦your.patches.app.category
 ├ 🔍Fingerprints.kt
 └ 🧩SomePatch.kt
```

> [!NOTE]
> Moving fingerprints to a separate file isn't strictly necessary, but it helps the organization when a patch uses multiple fingerprints.

## 📙 Conventions

- 🔥 Name a patch after what it does. For example, if a patch removes ads, name it `Remove ads`.
  If a patch changes the color of a button, name it `Change button color`
- 🔥 Write the patch description in the third person, present tense, and end it with a period.
  If a patch removes ads, the description can be omitted because of redundancy,
  but if a patch changes the color of a button, the description can be _Changes the color of the resume button to red._
- 🔥 Write patches with modularity and reusability in mind. Patches can depend on each other,
  so it is important to write patches in a way that can be used in different contexts.
- 🔥🔥 Keep patches as minimal as possible. This reduces the risk of failing patches.
  Instead of involving many abstract changes in one patch or writing entire methods or classes in a patch,
  you can write code in extensions. An extension is a precompiled DEX file that is merged into the patched app 
  before this patch is executed.
  Patches can then reference methods and classes from extensions.
  A real-world example of extensions can be found in the [ReVanced Patches](https://github.com/ReVanced/revanced-patches) repository
- 🔥🔥🔥 Do not overload a fingerprint with information about a method that's likely to change.
  In the example of an obfuscated method, it's better to fingerprint the method by its return type
  and parameters rather than its name because the name is likely to change. An intelligent selection
  of an opcode pattern or strings in a method can result in a strong fingerprint dynamic to app updates.
- 🔥🔥🔥 Document your patches. Patches are abstract, so it is important to document parts of the code
  that are not self-explanatory. For example, explain why and how a certain method is patched or large blocks
  of instructions that are modified or added to a method

## ⏭️ What's next

The next page discusses useful APIs for patch development.

Continue: [💪 Advanced APIs](4_apis.md)
