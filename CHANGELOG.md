# [1.0.0-dev.10](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.9...v1.0.0-dev.10) (2022-03-21)


### Code Refactoring

* Rename `net.revanced` to `app.revanced` ([f0ca1fb](https://github.com/ReVancedTeam/revanced-patcher/commit/f0ca1fb793557884d716954d2d4a3ecd4f1cb466))


### BREAKING CHANGES

* Package name was changed from "net.revanced" to "app.revanced"

# [1.0.0-dev.9](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.8...v1.0.0-dev.9) (2022-03-21)


### Bug Fixes

* **Io:** JAR loading and saving ([#8](https://github.com/ReVancedTeam/revanced-patcher/issues/8)) ([834b427](https://github.com/ReVancedTeam/revanced-patcher/commit/834b427fb7c801fd9cc8d616456d62639c58b1c0))

# [1.0.0-dev.8](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.7...v1.0.0-dev.8) (2022-03-21)


### Bug Fixes

* **gradle:** publish source and javadocs ([9ae5011](https://github.com/ReVancedTeam/revanced-patcher/commit/9ae5011ea9274cbc7ed397662d39f5ca93a59c6e))

# [1.0.0-dev.7](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.6...v1.0.0-dev.7) (2022-03-21)


### Bug Fixes

* nullable signature members ([#10](https://github.com/ReVancedTeam/revanced-patcher/issues/10)) ([ba256eb](https://github.com/ReVancedTeam/revanced-patcher/commit/ba256eb331da5dad4f5f1aff30bed9077fe78cd3))

# [1.0.0-dev.6](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.5...v1.0.0-dev.6) (2022-03-21)


### Features

* Add `findParentMethod` utility method ([#4](https://github.com/ReVancedTeam/revanced-patcher/issues/4)) ([632ae8a](https://github.com/ReVancedTeam/revanced-patcher/commit/632ae8a6b52cd224195225c1d7c2d4442937d9cb))

# [1.0.0-dev.5](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.4...v1.0.0-dev.5) (2022-03-20)


### Bug Fixes

* **Io:** fix finding classes by name ([1e1b522](https://github.com/ReVancedTeam/revanced-patcher/commit/1e1b5224fac33498c2bc47fc02f2a1b140961cb8))

# [1.0.0-dev.4](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.3...v1.0.0-dev.4) (2022-03-20)


### Bug Fixes

* set index for insertAt to 0 by default ([f7a6437](https://github.com/ReVancedTeam/revanced-patcher/commit/f7a6437c7903a08f272f9ec67def4816d1aa72bc))

# [1.0.0-dev.3](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.2...v1.0.0-dev.3) (2022-03-20)


### Bug Fixes

* Patch should have access to the Cache ([12c9b8f](https://github.com/ReVancedTeam/revanced-patcher/commit/12c9b8f5ba98ee9014193c4b47e24f0e0303d70d))


### BREAKING CHANGES

* Method signature of execute() was changed to include the cache, this will break existing implementations of the Patch class.

# [1.0.0-dev.2](https://github.com/ReVancedTeam/revanced-patcher/compare/v1.0.0-dev.1...v1.0.0-dev.2) (2022-03-20)


### Code Refactoring

* convert Patch to abstract class ([111b9c9](https://github.com/ReVancedTeam/revanced-patcher/commit/111b9c911fd149d11e0fa77683bae0a403c1bb4e))


### BREAKING CHANGES

* Patch class is now an abstract class. You must implement it. You can use anonymous implements, like done in the tests.

# 1.0.0-dev.1 (2022-03-20)


### Bug Fixes

* avoid ignoring test resources (fixes [#1](https://github.com/ReVancedTeam/revanced-patcher/issues/1)) ([d5a3c76](https://github.com/ReVancedTeam/revanced-patcher/commit/d5a3c76389ba902c22ddc8b7ba1a110b7ff852df))
* current must be calculated after increment ([5f12bab](https://github.com/ReVancedTeam/revanced-patcher/commit/5f12bab5df97fbe6e2e62c1bf2814a2e682ab4f3))
* remove broken code ([0e72a6e](https://github.com/ReVancedTeam/revanced-patcher/commit/0e72a6e85ff9a6035510680fc5e33ab0cd14144f))
* workflow on dev branch ([7e67daf](https://github.com/ReVancedTeam/revanced-patcher/commit/7e67daf8789c534bed0091a3975776eb95039acc))
