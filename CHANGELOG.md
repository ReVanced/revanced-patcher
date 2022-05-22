# [1.0.0-dev.11](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.10...v1.0.0-dev.11) (2022-05-22)


### Features

* `PatchLoader` ([1a99eca](https://github.com/revanced/revanced-patcher/commit/1a99ecaffe5e55977655316e68b014fdeba374a1))
* use annotations instead of metadata objects ([6726884](https://github.com/revanced/revanced-patcher/commit/6726884be5af56b6856749e73fb9f4f97559854a))

# [1.0.0-dev.10](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.9...v1.0.0-dev.10) (2022-05-07)


### Bug Fixes

* qualifying `Element` with wrong package ([4d74de4](https://github.com/revanced/revanced-patcher/commit/4d74de4061f26c0d7c17fabd849051b429d86033))

# [1.0.0-dev.9](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.8...v1.0.0-dev.9) (2022-05-07)


### Bug Fixes

* `compareSignatureToMethod` not matching correctly in case opcodes are null ([5ae5e98](https://github.com/revanced/revanced-patcher/commit/5ae5e98f1f8e174d800bcc75723e1ed965d66196))
* `ConcurrentModificationException` while iterating through `proxies` and modifying it ([bfeeaf4](https://github.com/revanced/revanced-patcher/commit/bfeeaf443549c9a43279d83a0628c061a382beb9))
* `PackageMetadata` ([305a817](https://github.com/revanced/revanced-patcher/commit/305a81793a9a04fe4e8969f2d3b591b0f01e3b63))
* `replaceWith` not replacing classes with used class proxies ([f0f3403](https://github.com/revanced/revanced-patcher/commit/f0f34031dd4e618223f016f7c427d7c93ab8456a))
* adding existing classes to the patchers cache ([4281546](https://github.com/revanced/revanced-patcher/commit/4281546f69225ee90ec4c003f4313df41edf71a6))
* always return PatchResultSuccess on patch success ([866b03a](https://github.com/revanced/revanced-patcher/commit/866b03af217ad97dd2755bfdc0ffe5bcf723c949))
* applying no patches throwing error ([f88c118](https://github.com/revanced/revanced-patcher/commit/f88c11820dbdc0d1d52a49c9bcdb4f7caa9eb6eb))
* applyPatches not returning successful patches ([8b70bb4](https://github.com/revanced/revanced-patcher/commit/8b70bb42909434a5e59315502f6d54d7c7691f18))
* Classes not being written properly because of array shifting ([1471956](https://github.com/revanced/revanced-patcher/commit/147195647c3990ab78ba95e4b3000650e718b713))
* failing tests temporarily ([66b08f8](https://github.com/revanced/revanced-patcher/commit/66b08f8b3a8f31844c7e7bab4df4243521d4a431))
* fix classes having multiple instances of fields ([b711b80](https://github.com/revanced/revanced-patcher/commit/b711b8001e4845857fa6cc71b107f1c553b31e80))
* fix classes having multiple method instances ([12c10d8](https://github.com/revanced/revanced-patcher/commit/12c10d8c64422c4534c23467e367707e3b953f82))
* Fixed writer & signature resolver, improved tests & speed, minor refactoring ([bb42fa3](https://github.com/revanced/revanced-patcher/commit/bb42fa3c6f59b78a7223fc70edbe598ec181ee37))
* fuzzy resolver warning params were turned around ([d49df10](https://github.com/revanced/revanced-patcher/commit/d49df10a3ca6b472ce4a32d10cfe787ca243d47b))
* incorrect pattern offset ([03700ff](https://github.com/revanced/revanced-patcher/commit/03700ffa519e5f20b1a0d0ffe68f3fb504351ee5))
* make `methodMetadata` nullable in `MethodSignatureMetadata` ([864e38c](https://github.com/revanced/revanced-patcher/commit/864e38c06906a9e29271fe383d51a8ec6594a46c))
* make warnings nullable instead of lateinit ([04b49b8](https://github.com/revanced/revanced-patcher/commit/04b49b8b664e45e64e9561eca3353ffdeda91187))
* match to correct signature method parameters ([c49071a](https://github.com/revanced/revanced-patcher/commit/c49071aff78245f27c98a4760b361c30aa6340bc))
* MethodSignature#resolved throwing an exception ([82b1e66](https://github.com/revanced/revanced-patcher/commit/82b1e66d54bed1e4c335e0515b7ff3ec901fa6f8))
* Move proxy package out of cache package ([6bc4e7e](https://github.com/revanced/revanced-patcher/commit/6bc4e7eab742f5796f3041332c70495e3f993c9b))
* null check causing an exception ([560c485](https://github.com/revanced/revanced-patcher/commit/560c485ab08b08a213b58704b11b1e2f5f625080))
* Patcher not writing resolved methods ([d15240d](https://github.com/revanced/revanced-patcher/commit/d15240d0330a63c4b568fc5de3de861b8046cba4))
* reaching all constructors not possible ([37fa994](https://github.com/revanced/revanced-patcher/commit/37fa9949ec84ffd277f32b1cd554e92be41d35e4))
* remove leftover debug code ([4458141](https://github.com/revanced/revanced-patcher/commit/4458141d6d2e1b015c0d70a6e65e6c32a3cf17dc))
* return mutable set of classes ([84bc7e0](https://github.com/revanced/revanced-patcher/commit/84bc7e0dc76f0732613383accb803f2c52da98ac))
* returning failure on success ([3b68d5c](https://github.com/revanced/revanced-patcher/commit/3b68d5c65ec3082d1aa48525b4ee2a4163895a3b))
* Search method map for existing class proxy ([d5e694c](https://github.com/revanced/revanced-patcher/commit/d5e694c306a47f47b8d1078b5c9f8a742445cf7e))
* string signature in `SignatureResolver` ([ac36d19](https://github.com/revanced/revanced-patcher/commit/ac36d19693390db8f404ed30963aefb2fb7519e0))
* Suppress unused for addFiles ([a0d6d46](https://github.com/revanced/revanced-patcher/commit/a0d6d462170552929039d71eafa813fdfde215cb))
* throwing in case the opcode patterns do not match ([f72dd68](https://github.com/revanced/revanced-patcher/commit/f72dd68ec575ee0926ee668911ebb6f85b75f7d1))
* use Array instead of Iterable for methodParameters ([312235b](https://github.com/revanced/revanced-patcher/commit/312235b194cac01ddc3f03ecff32c7de4e48c29c))
* write all classes ([6ad51aa](https://github.com/revanced/revanced-patcher/commit/6ad51aad9a94d8dd5afb5e270138ef7161ccfb07))


### Code Refactoring

* bump multidexlib2 to 2.5.2.r2 ([32e6458](https://github.com/revanced/revanced-patcher/commit/32e645850d4cc74aa708984da03ae1606e696d20))
* Change all references from Array to Iterable ([264989f](https://github.com/revanced/revanced-patcher/commit/264989f48804ed637469436acf8165ac4b7be383))


### Features

* add `MethodWalker` ([659e108](https://github.com/revanced/revanced-patcher/commit/659e1087c9e7a33e04cd7eb728c01ed946335810))
* add `p` naming scheme to smali compiler ([38556d6](https://github.com/revanced/revanced-patcher/commit/38556d61ab192dfa84083d935ee3e9eee5450d06))
* add extensions for cloning methods ([df7503b](https://github.com/revanced/revanced-patcher/commit/df7503b47b1e2162d6ab666f8586c633c314016f))
* add findClass method with className ([78235d1](https://github.com/revanced/revanced-patcher/commit/78235d1abe267e6aaa086662ad69af7132b8ff74))
* Add first tests ([6767c8f](https://github.com/revanced/revanced-patcher/commit/6767c8fbc15ea18a61db53e1472483632077f62a))
* add fuzzy resolver ([a492808](https://github.com/revanced/revanced-patcher/commit/a4928080217451017a99cf158fd5cc9d650a5a9e))
* add immutableMethod ([eed1cfd](https://github.com/revanced/revanced-patcher/commit/eed1cfda7b89f03f4c61ac4401707e1a12e6efb3))
* add inline smali compiler ([dbafe2a](https://github.com/revanced/revanced-patcher/commit/dbafe2ab37b25480f3e218d94ced5af2e56cba68))
* add missing test for fields ([4022b8b](https://github.com/revanced/revanced-patcher/commit/4022b8b847e8767ace0da3f98ad72ab61a4c242b))
* add or extension for AccessFlags ([aec5eeb](https://github.com/revanced/revanced-patcher/commit/aec5eeb597f0e9968b43efa228c96e83175e031c))
* Add patch metadata ([8544fc4](https://github.com/revanced/revanced-patcher/commit/8544fc4cbcb5d7c1ac0f6fcae52882a00d2bacf5)), closes [ReVancedTeam/revanced-patches#1](https://github.com/ReVancedTeam/revanced-patches/issues/1)
* Add warnings for Fuzzy resolver ([643a14e](https://github.com/revanced/revanced-patcher/commit/643a14e664c7ff86580da683eaff9c486884ee2c))
* allow classes to be overwritten in addFiles and resolve signatures when applyPatches is called ([5f71a34](https://github.com/revanced/revanced-patcher/commit/5f71a342ac9c6aa64a4983156f595ae0832c30e8))
* Allow unknown opcodes using `null` ([f4a47d4](https://github.com/revanced/revanced-patcher/commit/f4a47d4dc893bb511ca2087a1a63bfc35888663f))
* Finish first patcher test ([a9e4e8a](https://github.com/revanced/revanced-patcher/commit/a9e4e8ac3203bdd62abcd1e366f08a2269919571))
* Improve `SignatureResolver` ([88a6a27](https://github.com/revanced/revanced-patcher/commit/88a6a2730296883e191543c2666f39f24c05d74d))
* migrate to dexlib ([be51f42](https://github.com/revanced/revanced-patcher/commit/be51f42710c1489ef4405700e56ffecee5e6552f))
* Minor refactor and return proxy, if class has been proxied already ([2d3c611](https://github.com/revanced/revanced-patcher/commit/2d3c61113dc9b76c43e93928ba11026fe0ad444e))
* properly manage `ClassProxy` & add `ProxyBackedClassList` ([2319787](https://github.com/revanced/revanced-patcher/commit/23197879b20906aac7563e5f8107305edd7ccb1b))
* remaining mutable `EncodedValue` classes ([7d38bb0](https://github.com/revanced/revanced-patcher/commit/7d38bb0baaeabade6a9e64d97e2dd6c20edd153f))
* string signature ([#22](https://github.com/revanced/revanced-patcher/issues/22)) ([c245edb](https://github.com/revanced/revanced-patcher/commit/c245edb0c5317c1bb884ea315a1a04b720f20dd5))


### Performance Improvements

* depend on `androlib` instead of `ApkDecoder` ([e5c054a](https://github.com/revanced/revanced-patcher/commit/e5c054ac2f68b00ac123a45ed56b9f150332a82d))
* do not resolve empty signatures list ([1f7bf3a](https://github.com/revanced/revanced-patcher/commit/1f7bf3ac6c77a71abd687f2ff6f7306a40654a1b))
* lazy-ify all mutable clones ([05e4400](https://github.com/revanced/revanced-patcher/commit/05e44007d81399791aa1bab1eead66b7ff662043))
* optimize indexOf call away ([f8e978a](https://github.com/revanced/revanced-patcher/commit/f8e978af888255d9c104a8275be1d9b091af3f96))
* use Set instead of List since there are no dupes ([6221387](https://github.com/revanced/revanced-patcher/commit/622138736dca6c0161171330801b7b5666594ec7))
* use String List and compare instead of any lambda ([aed4fd9](https://github.com/revanced/revanced-patcher/commit/aed4fd9a3c9e7f96c1e2c54b831c3fe7d3d720a2))


### Reverts

* AccessFlag extensions not working with IDE ([e161f7f](https://github.com/revanced/revanced-patcher/commit/e161f7fea449883b7ac0fb436ed4f7f2ff78af62))
* previous commits check for dupes in dexFile, not cache ([433914f](https://github.com/revanced/revanced-patcher/commit/433914feda3066102a073d6b3bc457d0fae87911))


### BREAKING CHANGES

* arrayOf has to be changed to listOf.
* Method signature of Patcher#save() was changed to comply with the changes of multidexlib2.
* Removed usage of ASM library

# [1.0.0-dev.8](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.7...v1.0.0-dev.8) (2022-03-24)


### Performance Improvements

* check type instead of class ([47eb493](https://github.com/ReVancedTeam/revanced-patcher/commit/47eb493f5425dc27a4d6e79e6b02a36ef760e8da))

# [1.0.0-dev.7](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2022-03-24)


### Bug Fixes

* **MethodResolver:** fix cd57a8c9a0db7e3ae5ad0bca202e5955930319ab ([1af31b2](https://github.com/ReVancedTeam/revanced-patcher/commit/1af31b2aa3772a7473c04d27bf835c8eae13438d))

# [1.0.0-dev.6](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2022-03-24)


### Bug Fixes

* **MethodResolver:** strip labels nodes so opcode patterns match ([cd57a8c](https://github.com/ReVancedTeam/revanced-patcher/commit/cd57a8c9a0db7e3ae5ad0bca202e5955930319ab))

# [1.0.0-dev.5](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2022-03-24)


### Bug Fixes

* **MethodResolver:** strip labels and line numbers so opcode patterns match ([8d1bb5f](https://github.com/ReVancedTeam/revanced-patcher/commit/8d1bb5f3d9da544cf6e3e3848bfcc56327cde810))

# [1.0.0-dev.4](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2022-03-23)


### Bug Fixes

* give ClassWriter a ClassReader for symtable ([e8f6973](https://github.com/ReVancedTeam/revanced-patcher/commit/e8f6973938c70002f04a86f329aa5b134f6ef649))

# [1.0.0-dev.3](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2022-03-23)


### Features

* add SafeClassWriter ([ca6b94d](https://github.com/ReVancedTeam/revanced-patcher/commit/ca6b94d943b7067aae87a4e282cfb323811c0462))

# [1.0.0-dev.2](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2022-03-23)


### Bug Fixes

* set marklimit to Integer.MAX_VALUE ([ab6453c](https://github.com/ReVancedTeam/revanced-patcher/commit/ab6453ca8a02af70da4468c1a63c68dde4d392ef))

# 1.0.0-dev.1 (2022-03-23)


### Bug Fixes

* avoid ignoring test resources (fixes [#1](https://github.com/ReVancedTeam/revanced-patcher/issues/1)) ([d5a3c76](https://github.com/ReVancedTeam/revanced-patcher/commit/d5a3c76389ba902c22ddc8b7ba1a110b7ff852df))
* current must be calculated after increment ([5f12bab](https://github.com/ReVancedTeam/revanced-patcher/commit/5f12bab5df97fbe6e2e62c1bf2814a2e682ab4f3))
* **gradle:** publish source and javadocs ([87bbde5](https://github.com/ReVancedTeam/revanced-patcher/commit/87bbde5e06d038d8f6ddaac391e1db397f5a5590))
* **Io:** fix finding classes by name ([460d62a](https://github.com/ReVancedTeam/revanced-patcher/commit/460d62a24c4cad05691c4b269c2faeda47fee3b7))
* **Io:** JAR loading and saving ([#8](https://github.com/ReVancedTeam/revanced-patcher/issues/8)) ([4d98cbc](https://github.com/ReVancedTeam/revanced-patcher/commit/4d98cbc9e8fe1e39b3d9d4185b3c5b4882093af6))
* nullable signature members ([#10](https://github.com/ReVancedTeam/revanced-patcher/issues/10)) ([8db8893](https://github.com/ReVancedTeam/revanced-patcher/commit/8db8893ab1bda55f11cc75db55c7c1a38f1d1b16))
* Patch should have access to the Cache ([6c0f082](https://github.com/ReVancedTeam/revanced-patcher/commit/6c0f0823c91dc643dd80205b1e840e59827bee06))
* remove broken code ([0e72a6e](https://github.com/ReVancedTeam/revanced-patcher/commit/0e72a6e85ff9a6035510680fc5e33ab0cd14144f))
* set index for insertAt to 0 by default ([1769132](https://github.com/ReVancedTeam/revanced-patcher/commit/1769132a9e29cf3a0c5ae0917209c83c138c0216))
* workflow on dev branch ([7e67daf](https://github.com/ReVancedTeam/revanced-patcher/commit/7e67daf8789c534bed0091a3975776eb95039acc))


### Code Refactoring

* convert Patch to abstract class ([23e897a](https://github.com/ReVancedTeam/revanced-patcher/commit/23e897a7a9125f4ac4266263e7dd94fe63a0bfa1))
* Optimize Signature class ([#11](https://github.com/ReVancedTeam/revanced-patcher/issues/11)) ([49beec9](https://github.com/ReVancedTeam/revanced-patcher/commit/49beec9fc6eee6ccf52a6185761a200a6ed2b16e))
* Rename `net.revanced` to `app.revanced` ([3ab42a9](https://github.com/ReVancedTeam/revanced-patcher/commit/3ab42a932c8d5027d554106dfe8e1299ebc1ac7f))


### Features

* Add `findParentMethod` utility method ([#4](https://github.com/ReVancedTeam/revanced-patcher/issues/4)) ([00c6ab7](https://github.com/ReVancedTeam/revanced-patcher/commit/00c6ab7fafe2a59dec0052cc5b7d1d16939076b2))


### BREAKING CHANGES

* Array<Int> was changed to IntArray. This breaks existing patches.
* Package name was changed from "net.revanced" to "app.revanced"
* Method signature of execute() was changed to include the cache, this will break existing implementations of the Patch class.
* Patch class is now an abstract class. You must implement it. You can use anonymous implements, like done in the tests.
