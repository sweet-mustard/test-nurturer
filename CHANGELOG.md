<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# test-nurturer Changelog

## [Unreleased]

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

[Unreleased]: https://github.com/sweet-mustard/test-nurturer/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.3...v0.1.0
[0.0.3]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/sweet-mustard/test-nurturer/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/sweet-mustard/test-nurturer/commits/v0.0.1
