# BandLab Android IntelliJ Plugin

<!-- Plugin description -->

This plugin offers a suite of features to help developers work more efficiently with the BandLab Android project.

Please note that this plugin is not available for external use; we've open-sourced it solely to demonstrate our approach to improving develop experience through IDE integration.

## Module Creation
- Create modules following BandLab Android conventions (:api, :impl, :ui, :screen).
- Support for batch creation of multiple modules.
- Contextual module path pre-filling and autocomplete.
- Optional `:impl` and `:screen` exposure to `AppGraph` (default) or `MixEditorGraph`.
- Templates for Activities and Pages.
- Support adding to spotlight after creating modules.

![Module Creation Wizard](https://i.imgur.com/nD1wqPw.png)

## Templates
![Templates](https://i.imgur.com/CGQc12d.png)

### Activity Template
Generates Activity, ViewModel, and Manifest per latest conventions.

### Page Template
Generates Page and ViewModel.

### Two-level Injection template
Generates an interface and an impl for 2-level injection. Learn more about two-level injection [here](https://bandlab.atlassian.net/wiki/spaces/Android/pages/3294724236/Dependency+Injection#Two-level-Injection).

### Automation Templates
![Automation Templates](https://i.imgur.com/Vr0Gkrl.png)
Generate Robot, Semantics, and Verifier templates following our automation conventions. Available only under the `androidTest` source set.

## Module Analyzer
![module analyzer](https://i.imgur.com/kuYqEDV.png)

Right-click a module to analyze it using [DAGP](https://github.com/autonomousapps/dependency-analysis-gradle-plugin) and our internal scoring plugin (predicts JVM module compatibility).

The plugin helps invoke these tasks in your IDE's Run tab:
- **Android modules:** `./gradlew :module:analyzeModule`
- **Non-Android modules:** `./gradlew :module:projectHealth`

## build.gradle Actions
![build.gradle actions](https://i.imgur.com/MW2zART.png)

### Dependency Sorting
Right-click `build.gradle` to sort plugins and dependencies alphabetically.

### Test Fixtures Plugin
Right-click `build.gradle` to apply the Test Fixtures plugin and automatically create the required folders.

### Project Path Autocomplete
![auto-complete project path](https://i.imgur.com/IcXx7Rm.png)

Since we avoid Gradle [type-safe accessors](https://www.zacsweers.dev/dont-use-type-safe-project-accessors-with-kotlin-gradle-dsl/), this plugin provides autocomplete and validation for project paths. Invalid paths are highlighted with a red underline.

_Disclaimer: This feature is copied from [Slack foundry](https://github.com/slackhq/foundry/pull/1440)._

<!-- Plugin description end -->

## Installation

Add the following URL to [your plugin configuration](https://www.jetbrains.com/help/idea/managing-plugins.html#add_plugin_repos) 
and search for "BandLab", you'll see our shiny plugin!
```
https://artifactory.bandlab.cloud/artifactory/intellij-idea-plugins/updatePlugins.xml
```

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
