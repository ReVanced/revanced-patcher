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
