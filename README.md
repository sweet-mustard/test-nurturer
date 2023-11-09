# test-nurturer

![Build](https://github.com/wimdeblauwe/test-nurturer/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/23056-test-nurturer.svg)](https://plugins.jetbrains.com/plugin/23056-test-nurturer)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/23056-test-nurturer.svg)](https://plugins.jetbrains.com/plugin/23056-test-nurturer)

<!-- Plugin description -->
The Test Nurturer IntelliJ-based plugin allows generating Test Mother classes.
This plugin was inspired by the [Mastering the Object Mother](https://jonasg.io/posts/object-mother/) blog post of [Jonas Geiregat](https://jonasg.io/).

After installing the plugin, you can generate a Test Mother via the Tools > Generate Test Mother
menu item.

The plugin also contains an inspection that will warn if the Test Mother is missing fields that are
present in the object that you create a Test Mother for.

The development of this plugin is proudly sponsored by [Sweet Mustard](https://www.sweetmustard.be/).
<!-- Plugin description end -->

## Adding a new feature

1. Create an issue in the [issue tracker](https://github.com/sweet-mustard/test-nurturer/issues), or
   pick up one of the open issues. Assign yourself so it is clear somebody is working on it.
2. Create a branch called `feature/gh-xxx` where `xxx` is the issue number on GitHub.
3. Commit on the branch and push the branch.
4. When done, update `CHANGLELOG.md` with information on the change you have done. Also make sure
   you are up-to-date with the latest changes on `main`.
5. Open a [pull request](https://github.com/sweet-mustard/test-nurturer/pulls) so the change can be
   reviewed.
6. If the PR is approved, it will be merged to `main` and included in the next release.

## Releasing a new version

1. Ensure all PR's are merged into the `main` branch.
2. Update `pluginVersion` in the `gradle.properties` file to have the correct version you want to
   release.
3. Open the [GitHub releases](https://github.com/sweet-mustard/test-nurturer/releases) page. There
   should be a draft release there.
4. Edit the draft release and press "Publish release". This will start
   the [release workflow](https://github.com/sweet-mustard/test-nurturer/actions/workflows/release.yml).
5. When the release action is done,
   a [pull request](https://github.com/sweet-mustard/test-nurturer/pulls) will be created with the
   changes to the `CHANGELOG.md` that will have the proper version filled in.
6. Wait for the version to be approved by JetBrains.
   See https://plugins.jetbrains.com/plugin/23056-test-nurturer for the plugin page.

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "test-nurturer"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/sweetmustard/test-nurturer/releases/latest) and
  install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
