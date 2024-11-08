<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# test-nurturer Changelog

## [Unreleased]

## [2.1.0] - 2024-11-08

- Bump org.jetbrains.kotlin.jvm from 2.0.10 to 2.0.20 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/76
- Bump JetBrains/qodana-action from 2024.1.9 to 2024.2.3 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/80
- Bump org.jetbrains.qodana from 2024.1.9 to 2024.2.3 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/79
- Bump org.jetbrains.intellij.platform from 2.0.1 to 2.1.0 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/81
- Bump org.jetbrains:annotations from 24.1.0 to 25.0.0 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/82
- Bump org.jetbrains.kotlin.jvm from 2.0.20 to 2.0.21 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/84
- Bump org.jetbrains:annotations from 25.0.0 to 26.0.1 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/85
- Bump JetBrains/qodana-action from 2024.2.3 to 2024.2.6 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/88
- Bump org.jetbrains.qodana from 2024.2.3 to 2024.2.6 by @dependabot in https://github.com/sweet-mustard/test-nurturer/pull/89

## [2.0.0] - 2024-08-16

### Changed

- Migrate
  to [IntelliJ Platform Gradle Plugin 2.0](https://blog.jetbrains.com/platform/2024/07/intellij-platform-gradle-plugin-2-0/).
- Make plugin compatible with IntelliJ IDEA 2024.2

## [1.0.0] - 2024-04-29

### Fixed

- Make plugin compatible with IntelliJ IDEA 2024.1

## [0.1.0] - 2023-12-07

### Added

- A Live Template called `bm` is added automatically to quickly generate a builder method manually.

### Fixed

- The inspection will no longer report that there is no field in the corresponding test mother for
  static fields.
- Fixed NullPointerException when opening a project.
- Fixed PluginException when a field was missing that was declared in a parent class.

## [0.0.3] - 2023-11-13

### Added

- Show popup when invoking the action to allow the user to either jump to the test mother, or update
  the test mother.
- Show a popup to select a test source root to generate the test mother in if there are multiple
  test source roots.

### Changed

- The generated Test Mother class is now `final`.

## [0.0.2] - 2023-11-09

### Changed

- Updated to Kotlin 1.9.20

## [0.0.1]

### Added

- Initial scaffold created
  from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/sweet-mustard/test-nurturer/compare/v2.1.0...HEAD
[2.1.0]: https://github.com/sweet-mustard/test-nurturer/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/sweet-mustard/test-nurturer/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/sweet-mustard/test-nurturer/compare/v0.1.0...v1.0.0
[0.1.0]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.3...v0.1.0
[0.0.3]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/sweet-mustard/test-nurturer/commits/v0.0.1
