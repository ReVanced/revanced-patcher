## [1.2.1](https://github.com/revanced/revanced-patcher/compare/v1.2.0...v1.2.1) (2022-06-14)


### Bug Fixes

* Patcher setting BuildOptions too late ([6a5c873](https://github.com/revanced/revanced-patcher/commit/6a5c8735fb8a5d6f7e9c606734b6684c7fa99e7f))

# [1.2.0](https://github.com/revanced/revanced-patcher/compare/v1.1.0...v1.2.0) (2022-06-14)


### Features

* allow custom framework path to be specified ([d3a580e](https://github.com/revanced/revanced-patcher/commit/d3a580ea19d7c2d5d8c97650b1e6396ea0a7fc25))

# [1.1.0](https://github.com/revanced/revanced-patcher/compare/v1.0.0...v1.1.0) (2022-06-11)


### Bug Fixes

* resource patcher ([31815ca](https://github.com/revanced/revanced-patcher/commit/31815ca9ea990f16b3600d61fd570c1805be1c82))
* update apktool to fork ([566ecef](https://github.com/revanced/revanced-patcher/commit/566ecefa2bd4cde5ebfb2b22dc56cd8bf9f396bd))


### Features

* allow custom aapt path to be specified ([8eb4a8f](https://github.com/revanced/revanced-patcher/commit/8eb4a8f87ae7679a272f3224273a37a31d4bb121))

# 1.0.0 (2022-06-05)


### Bug Fixes

* `compareSignatureToMethod` not matching correctly in case opcodes are null ([cca12aa](https://github.com/revanced/revanced-patcher/commit/cca12aa34a60d766c02e55241df847f7d230d4d7))
* `ConcurrentModificationException` while iterating through `proxies` and modifying it ([6cb7cdb](https://github.com/revanced/revanced-patcher/commit/6cb7cdb0b2a2b954adb04033e0f2d3ccb4604545))
* `JarPatchBundle` loading non-class files to class loader ([849616d](https://github.com/revanced/revanced-patcher/commit/849616dc2b6e30ec1fa1d8a8f9c1f881fc11676a))
* `PackageMetadata` ([7399450](https://github.com/revanced/revanced-patcher/commit/739945013962fd80d2635fff126d84046870f956))
* `replaceWith` not replacing classes with used class proxies ([4178a1e](https://github.com/revanced/revanced-patcher/commit/4178a1eedce1436ffeb3ddd6952ce0b6ec87d5a0))
* adding existing classes to the patchers cache ([9659a61](https://github.com/revanced/revanced-patcher/commit/9659a61c5c3a84714160b78b32cc337a97c8caa9))
* always return PatchResultSuccess on patch success ([996c4ac](https://github.com/revanced/revanced-patcher/commit/996c4acb2061db776430ad8b07bfdb3fe32861f6))
* applying no patches throwing error ([5ca5a1c](https://github.com/revanced/revanced-patcher/commit/5ca5a1c29e087ce7e4b6d5e593b775365803151d))
* applyPatches not returning successful patches ([f806cb3](https://github.com/revanced/revanced-patcher/commit/f806cb38c571cdd22016396ee1874ee18c91b79f))
* avoid ignoring test resources (fixes [#1](https://github.com/revanced/revanced-patcher/issues/1)) ([d5a3c76](https://github.com/revanced/revanced-patcher/commit/d5a3c76389ba902c22ddc8b7ba1a110b7ff852df))
* Classes not being written properly because of array shifting ([6e4db11](https://github.com/revanced/revanced-patcher/commit/6e4db110c8fdd16fb0c0ce81f427d84f2a3b6ee0))
* current must be calculated after increment ([5f12bab](https://github.com/revanced/revanced-patcher/commit/5f12bab5df97fbe6e2e62c1bf2814a2e682ab4f3))
* failing tests temporarily ([fc05fe7](https://github.com/revanced/revanced-patcher/commit/fc05fe79deec2486bb746d33e803ad052e68f8de))
* fix classes having multiple instances of fields ([7cc8a7d](https://github.com/revanced/revanced-patcher/commit/7cc8a7dec321774c1d3f2f1a87ac91f952c4fb7e))
* fix classes having multiple method instances ([398239d](https://github.com/revanced/revanced-patcher/commit/398239dc10a3ea04e46adb3be176c897876e5587))
* Fixed writer & signature resolver, improved tests & speed, minor refactoring ([e6c2501](https://github.com/revanced/revanced-patcher/commit/e6c2501539540301d5b70014de460e5452a09b04))
* fuzzy resolver warning params were turned around ([e5bea06](https://github.com/revanced/revanced-patcher/commit/e5bea06353805f004d607124a8ebed138f84d583))
* give ClassWriter a ClassReader for symtable ([41749ba](https://github.com/revanced/revanced-patcher/commit/41749ba8290b2dec5dd2ab6e0bc9d714887a1a05))
* **gradle:** publish source and javadocs ([c236ebe](https://github.com/revanced/revanced-patcher/commit/c236ebe0789f9c78d610769f0feda2b64fa4a128))
* incorrect pattern offset ([f3b5f67](https://github.com/revanced/revanced-patcher/commit/f3b5f67b395167c1b9411b2374f3ef584b57b6cf))
* **Io:** fix finding classes by name ([b957501](https://github.com/revanced/revanced-patcher/commit/b957501e709028005c4d6c7857022980205b6861))
* **Io:** JAR loading and saving ([#8](https://github.com/revanced/revanced-patcher/issues/8)) ([310a7c4](https://github.com/revanced/revanced-patcher/commit/310a7c446b547d84b02c5da2161958e77ce69f0d))
* make `methodMetadata` nullable in `MethodSignatureMetadata` ([4e56652](https://github.com/revanced/revanced-patcher/commit/4e566524299674426fb0344d09db3b0c1cb3d300))
* make warnings nullable instead of lateinit ([8f1a629](https://github.com/revanced/revanced-patcher/commit/8f1a629191668e05917dc797e486647e55276d59))
* match to correct signature method parameters ([1ee2e4b](https://github.com/revanced/revanced-patcher/commit/1ee2e4ba56097c5e06c93c9ce04cb5543f0e4a67))
* **MethodResolver:** fix cd57a8c9a0db7e3ae5ad0bca202e5955930319ab ([cbd8df2](https://github.com/revanced/revanced-patcher/commit/cbd8df2df008ef37c6b43e2a8442c41f24be9358))
* **MethodResolver:** strip labels and line numbers so opcode patterns match ([699c730](https://github.com/revanced/revanced-patcher/commit/699c730a7cecf31878827d645e845490a37de4cb))
* **MethodResolver:** strip labels nodes so opcode patterns match ([82c5306](https://github.com/revanced/revanced-patcher/commit/82c530650f926dd026d263cfe23a7d67cb27bbf2))
* MethodSignature#resolved throwing an exception ([c612676](https://github.com/revanced/revanced-patcher/commit/c612676543282155143471b71a095e26023806ea))
* Move proxy package out of cache package ([ce21bd6](https://github.com/revanced/revanced-patcher/commit/ce21bd60f34d78b94d6d85f2c5375bc934ed4091))
* null check causing an exception ([338bd9f](https://github.com/revanced/revanced-patcher/commit/338bd9f7394afd84e5e195a7f8155c813812cfb5))
* nullable signature members ([#10](https://github.com/revanced/revanced-patcher/issues/10)) ([674461f](https://github.com/revanced/revanced-patcher/commit/674461f08daabbf92cb54e4eadb408226fac47af))
* Patch should have access to the Cache ([4dd820f](https://github.com/revanced/revanced-patcher/commit/4dd820ffdf1b98fe41b50f7cb2670b89acfbb99d))
* Patcher not writing resolved methods ([fac44a5](https://github.com/revanced/revanced-patcher/commit/fac44a50c39d8c102bd3e7ca4dd1bb86d29f7b57))
* qualifying `Element` with wrong package ([024fa86](https://github.com/revanced/revanced-patcher/commit/024fa867e115f984cfa3e395b78f4f43aa81709b))
* reaching all constructors not possible ([c459beb](https://github.com/revanced/revanced-patcher/commit/c459beb5f898d797f2f03ed36326bd9cfad03d31))
* reformat (trigger release) ([bf48945](https://github.com/revanced/revanced-patcher/commit/bf4894592bf9ee9c6233abc91f538b7b8ef986a0))
* remove broken code ([0e72a6e](https://github.com/revanced/revanced-patcher/commit/0e72a6e85ff9a6035510680fc5e33ab0cd14144f))
* remove dependency to fork of Apktool ([11abc67](https://github.com/revanced/revanced-patcher/commit/11abc67d9ab7d7b273fd4cd4c53af54008a80585))
* remove leftover debug code ([0f30eac](https://github.com/revanced/revanced-patcher/commit/0f30eac32ce66d8b90906c02ef7e7854feeecc33))
* return mutable set of classes ([66a9b76](https://github.com/revanced/revanced-patcher/commit/66a9b768457e98fdde0b61f9a8d6aed4c1872027))
* returning failure on success ([48c4ea2](https://github.com/revanced/revanced-patcher/commit/48c4ea2f6d9de319383a49ea2d4c6ffb4f687a2b))
* Search method map for existing class proxy ([a1e909b](https://github.com/revanced/revanced-patcher/commit/a1e909b16337c538f8f8b475801d8b1804163bfe))
* set index for insertAt to 0 by default ([d5b4c99](https://github.com/revanced/revanced-patcher/commit/d5b4c99c00272e3e5afec2fa0a489ba618f2a81a))
* set marklimit to Integer.MAX_VALUE ([e6e468f](https://github.com/revanced/revanced-patcher/commit/e6e468fbb5c20b08c8bd59bafc794acea907e4b4))
* string signature in `SignatureResolver` ([e5ae970](https://github.com/revanced/revanced-patcher/commit/e5ae9700096924e63b15a08079dce40ae07202d8))
* Suppress unused for addFiles ([3d6a1d3](https://github.com/revanced/revanced-patcher/commit/3d6a1d38f339ce2c5d82b7ac46c208c6702d6d44))
* throwing in case the opcode patterns do not match ([3144ec8](https://github.com/revanced/revanced-patcher/commit/3144ec872ac8651b8c0a9311ae508d5c3cc734ce))
* use Array instead of Iterable for methodParameters ([dfac8f0](https://github.com/revanced/revanced-patcher/commit/dfac8f03a362fd273527f552d9eae121505fd4e0))
* using old instance of `Androlib` when saving ([a4d8be2](https://github.com/revanced/revanced-patcher/commit/a4d8be20fcd444b08ec9c43f9f7029f8bacbbc41))
* workflow on dev branch ([428f7f4](https://github.com/revanced/revanced-patcher/commit/428f7f4decb00d28c9bf137ef4cd1d5fd4a0821e))
* write all classes ([f068fc8](https://github.com/revanced/revanced-patcher/commit/f068fc87ff8e204826639318af39e48e683254da))


### Code Refactoring

* bump multidexlib2 to 2.5.2.r2 ([a6c6b49](https://github.com/revanced/revanced-patcher/commit/a6c6b4979af42936cb26608541a4f7a66393b3f0))
* Change all references from Array to Iterable ([72f3cad](https://github.com/revanced/revanced-patcher/commit/72f3cad3f98001b0109b07373ed9cc57a9001cfa))
* convert Patch to abstract class ([cb9b1b9](https://github.com/revanced/revanced-patcher/commit/cb9b1b9416c699c68d0fca228d4f8ca6fb634cb5))
* Optimize Signature class ([#11](https://github.com/revanced/revanced-patcher/issues/11)) ([7faa001](https://github.com/revanced/revanced-patcher/commit/7faa001406c1f28dc2182cf6d1ab19504f4e3eb9))
* Rename `net.revanced` to `app.revanced` ([7087230](https://github.com/revanced/revanced-patcher/commit/70872307e33282b37dd5fb315b56022ab73bf582))


### Features

* `Dependencies` annotation ([893d4c6](https://github.com/revanced/revanced-patcher/commit/893d4c699bad4c70002fc691c261447d01948b5c))
* `PatchLoader` ([ec9fd15](https://github.com/revanced/revanced-patcher/commit/ec9fd15f9b9b9968be7fb5cb384eb8ee2a0c9ba3))
* Add `findParentMethod` utility method ([#4](https://github.com/revanced/revanced-patcher/issues/4)) ([bbb2c54](https://github.com/revanced/revanced-patcher/commit/bbb2c547aae8dd774a1a883de24fe45da463fa35))
* add `MethodWalker` ([7755bbc](https://github.com/revanced/revanced-patcher/commit/7755bbc645773e49053fb9ad2b6fd18a7f488659))
* add `p` naming scheme to smali compiler ([79909cf](https://github.com/revanced/revanced-patcher/commit/79909cf260c0578e88ad22d63397957dbaa91702))
* add extensions for cloning methods ([01bfbd6](https://github.com/revanced/revanced-patcher/commit/01bfbd656ee06cb2cab951c43d7f76a465a40830))
* add findClass method with className ([4087f49](https://github.com/revanced/revanced-patcher/commit/4087f498638ee88ba3eaca792039fe481f404732))
* Add first tests ([544bcf7](https://github.com/revanced/revanced-patcher/commit/544bcf76bd8a8c790c2f799606ad8c9ac7d2aa82))
* add fuzzy resolver ([7a56dca](https://github.com/revanced/revanced-patcher/commit/7a56dca004cd793121a59ea854c77f4c1a01bd6f))
* add immutableMethod ([c63b20f](https://github.com/revanced/revanced-patcher/commit/c63b20fa65aba8bb060a4a7a652747cba7198c2b))
* add inline smali compiler ([bfe4e3e](https://github.com/revanced/revanced-patcher/commit/bfe4e3e298ac963936ca9621e12aefbe56260826))
* add missing test for fields ([6b8b057](https://github.com/revanced/revanced-patcher/commit/6b8b0573d479e227b45dc36a6abac622c3ccebdd))
* add or extension for AccessFlags ([00c85b5](https://github.com/revanced/revanced-patcher/commit/00c85b5d750ccc8de69ad4101220b19eeaf99bcb))
* Add patch metadata ([642e903](https://github.com/revanced/revanced-patcher/commit/642e9031eb3727ebdca22c75b7c5c602a8775da0)), closes [ReVancedTeam/revanced-patches#1](https://github.com/ReVancedTeam/revanced-patches/issues/1)
* add SafeClassWriter ([6626014](https://github.com/revanced/revanced-patcher/commit/6626014ef3dde2f98a53f75d71eeb0de85189bf3))
* Add warnings for Fuzzy resolver ([715a2ad](https://github.com/revanced/revanced-patcher/commit/715a2ad025d127b5a8225ce50202a859f53c7f50))
* allow classes to be overwritten in addFiles and resolve signatures when applyPatches is called ([1db735b](https://github.com/revanced/revanced-patcher/commit/1db735b1e2b570bdb1ddce0b9cd724c580113a84))
* Allow unknown opcodes using `null` ([0e5f4ba](https://github.com/revanced/revanced-patcher/commit/0e5f4ba2d55288415c4d1be70ab6a8ab8c1c0d10))
* Finish first patcher test ([0d8d19e](https://github.com/revanced/revanced-patcher/commit/0d8d19e708a47315e28e7493618568ea40f1e062))
* Improve `SignatureResolver` ([139a23b](https://github.com/revanced/revanced-patcher/commit/139a23b7500a2d2577df47caf3fd0c5ec891a8d8))
* migrate to `DexPatchBundle` and `JarPatchBundle` ([8615798](https://github.com/revanced/revanced-patcher/commit/8615798711185b30ce622d9d09faba21f3a92f97))
* migrate to dexlib ([3651981](https://github.com/revanced/revanced-patcher/commit/36519811610192e299834e9d00627a94faad56a9))
* Minor refactor and return proxy, if class has been proxied already ([4b26305](https://github.com/revanced/revanced-patcher/commit/4b26305bd57ba9e3eb3e34218ffe10d6c5a2f598))
* optional `forStaticMethod` parameter for `InlineSmaliCompiler.compileMethodInstructions` ([41e8860](https://github.com/revanced/revanced-patcher/commit/41e88605c33d1f0d9e7f5466cac03a3b339afb82))
* patch dependencies annotation and `PatcherOptions` ([6c65952](https://github.com/revanced/revanced-patcher/commit/6c65952d80a795a3ef4a37877123e9375025d3ae))
* properly manage `ClassProxy` & add `ProxyBackedClassList` ([6cb1fdf](https://github.com/revanced/revanced-patcher/commit/6cb1fdf6171e1ab75b7ee28163965eacc00cc5a0))
* remaining mutable `EncodedValue` classes ([3f97cc8](https://github.com/revanced/revanced-patcher/commit/3f97cc8e1fa10546d7069e01e5e66a537b0d6f7e))
* string signature ([#22](https://github.com/revanced/revanced-patcher/issues/22)) ([612515a](https://github.com/revanced/revanced-patcher/commit/612515acf8539febf952f258d30aa3d4b631e3b7))
* use annotations instead of metadata objects ([d20f7fd](https://github.com/revanced/revanced-patcher/commit/d20f7fd6e1ede6ec7baccb1500ab3fc66d78df73))
* utility functions to get metadata of patch & sigs ([54511a4](https://github.com/revanced/revanced-patcher/commit/54511a4fc6417d7fe0c868d441e7d6b0ec9e218d))


### Performance Improvements

* check type instead of class ([c7ef264](https://github.com/revanced/revanced-patcher/commit/c7ef2644d83e1d8e84decb0631a6549d394180fc))
* decode manifest only when not using resource patcher ([4f60bea](https://github.com/revanced/revanced-patcher/commit/4f60bea81e0bbe85dc6c3150238980292a1e52ab))
* depend on `androlib` instead of `ApkDecoder` ([cc9416d](https://github.com/revanced/revanced-patcher/commit/cc9416dd11b66140c2882021cbe5088659d85371))
* do not resolve empty signatures list ([b1eebc9](https://github.com/revanced/revanced-patcher/commit/b1eebc99a71269df33c37f35c1f56ea20a9d6bc0))
* lazy-ify all mutable clones ([d18a3b6](https://github.com/revanced/revanced-patcher/commit/d18a3b6a28cae4fcb1c4986903208298ee50b083))
* optimize indexOf call away ([9991f39](https://github.com/revanced/revanced-patcher/commit/9991f39c9a4fa22a221aab0bbf9e08ca7f967fa9))
* use Set instead of List since there are no dupes ([e65ebd2](https://github.com/revanced/revanced-patcher/commit/e65ebd27c250b1735acf73af0f6b03274b0137f6))
* use String List and compare instead of any lambda ([5bd416b](https://github.com/revanced/revanced-patcher/commit/5bd416b409290906a6378344f70391e8692ae27f))


### Reverts

* AccessFlag extensions not working with IDE ([0bfb92a](https://github.com/revanced/revanced-patcher/commit/0bfb92a0cbd72df5ba513264efb583e201cfcf82))
* previous commits check for dupes in dexFile, not cache ([e810197](https://github.com/revanced/revanced-patcher/commit/e810197e2aa64534f2e8637165d884cbefbce8ae))


### BREAKING CHANGES

* arrayOf has to be changed to listOf.
* Method signature of Patcher#save() was changed to comply with the changes of multidexlib2.
* Removed usage of ASM library
* Array<Int> was changed to IntArray. This breaks existing patches.
* Package name was changed from "net.revanced" to "app.revanced"
* Method signature of execute() was changed to include the cache, this will break existing implementations of the Patch class.
* Patch class is now an abstract class. You must implement it. You can use anonymous implements, like done in the tests.

# [1.0.0-dev.18](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.17...v1.0.0-dev.18) (2022-06-04)


### Features

* `Dependencies` annotation ([83d608a](https://github.com/revanced/revanced-patcher/commit/83d608ac06a7d5ceb31b6e0022b501d99edb63a3))
* optional `forStaticMethod` parameter for `InlineSmaliCompiler.compileMethodInstructions` ([28b9847](https://github.com/revanced/revanced-patcher/commit/28b98478e4e8e8f238e82f7fa2307aeb1547955d))

# [1.0.0-dev.17](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.16...v1.0.0-dev.17) (2022-05-31)


### Features

* patch dependencies annotation and `PatcherOptions` ([8442991](https://github.com/revanced/revanced-patcher/commit/84429912900872405b44804943357dda8430a550))

# [1.0.0-dev.16](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.15...v1.0.0-dev.16) (2022-05-27)


### Bug Fixes

* `JarPatchBundle` loading non-class files to class loader ([3f0c740](https://github.com/revanced/revanced-patcher/commit/3f0c740200dd91a060426638c2f8f516938b4c53))
* remove dependency to fork of Apktool ([0fa529f](https://github.com/revanced/revanced-patcher/commit/0fa529fcdf9a7b5ea9a361b9f9f32f3f3fce009f))


### Features

* migrate to `DexPatchBundle` and `JarPatchBundle` ([7573db2](https://github.com/revanced/revanced-patcher/commit/7573db25757de89824af4f3aea167e500120eabb))

# [1.0.0-dev.15](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.14...v1.0.0-dev.15) (2022-05-25)


### Features

* utility functions to get metadata of patch & sigs ([72f16b7](https://github.com/revanced/revanced-patcher/commit/72f16b778587c28d8f8e91da502f197e7dc35d6d))

# [1.0.0-dev.14](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.13...v1.0.0-dev.14) (2022-05-24)


### Bug Fixes

* reformat (trigger release) ([45a167e](https://github.com/revanced/revanced-patcher/commit/45a167e7856da0306f796953775c7b7543d9bec0))

# [1.0.0-dev.13](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.12...v1.0.0-dev.13) (2022-05-24)


### Performance Improvements

* decode manifest only when not using resource patcher ([40b1fa4](https://github.com/revanced/revanced-patcher/commit/40b1fa43e1704ace29d3e349df2f4a8ea828c5c2))

# [1.0.0-dev.12](https://github.com/revanced/revanced-patcher/compare/v1.0.0-dev.11...v1.0.0-dev.12) (2022-05-22)


### Bug Fixes

* using old instance of `Androlib` when saving ([5630e49](https://github.com/revanced/revanced-patcher/commit/5630e4966310311cdfd53e2ba128255047626adc))

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
