# 👶 Preparing a development environment

To get started developing patches with ReVanced Patcher, you need to prepare a development environment.

## 📝 Prerequisites

- A Java IDE with Kotlin support, such as [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- Knowledge of Java, [Kotlin](https://kotlinlang.org), and [Dalvik bytecode](https://source.android.com/docs/core/runtime/dalvik-bytecode)
- Android reverse engineering skills and tools such as [jadx](https://github.com/skylot/jadx)

## 🏃 Prepare the environment

Throughout the documentation, [ReVanced Patches](https://github.com/revanced/revanced-patches) will be used as a base.

1. Clone the repository

   ```bash
   git clone https://github.com/revanced/revanced-patches && cd revanced-patches
   ```

2. Build the project

   ```bash
   ./gradlew build
   ```

   > [!NOTE]
   > If the build fails due to authenticate, you may need to authenticate yourself to GitHub Packages.
   > Create a PAT with the scope `read:packages` [here](https://github.com/settings/tokens/new?scopes=read:packages&description=ReVanced) and add your token to ~/.gradle/gradle.properties.
   >
   > Example `gradle.properties` file:
   >
   > ```properties
   > gpr.user = user
   > gpr.key = key
   > ```

3. Open the project in your IDE

> [!TIP]
> It is a good idea to setup a complete development environment for ReVanced, so that you can also test your patches by following the [ReVanced documentation](https://github.com/ReVanced/revanced-documentation).

## ⏭️ What's next

The next page will go into details about a ReVanced patch.

Continue: [🧩 Anatomy of a patch](2_anatomy.md)
