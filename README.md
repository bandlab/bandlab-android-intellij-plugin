# BandLab Android IntelliJ Plugin

<!-- Plugin description -->

This plugin offers a suite of features to help developers work more efficiently with the BandLab Android project.

Please note that this plugin is not available for external use; we've open-sourced it solely to demonstrate our approach to improving develop experience through IDE integration.

## Module Creation
![Module Creation Wizard](https://i.imgur.com/2an7NI7.png)

**The UI is implemented using [Jewel](https://github.com/JetBrains/intellij-community/tree/master/platform/jewel) (Compose Desktop)!** 🔮

- Create modules following BandLab Android conventions (:api, :impl, :ui, :screen).
- Support for batch creation of multiple modules.
- Contextual module path pre-filling and autocomplete.
- Optional `:impl` and `:screen` exposure to `AppGraph` (default) or `MixEditorGraph`.
- Templates for Activities and Pages.
- Support adding to spotlight after creating modules.

---

## Templates
![Templates](https://i.imgur.com/CGQc12d.png)

### Activity Template
Generates Activity, ViewModel, and Manifest per latest conventions.

### Page Template
Generates Page and ViewModel.

_Notes: Page is our internal framework to render a Composable with a given injected ViewModel type._

```kotlin
interface Page<ViewModel : Any> {

    @Composable
    fun Content(viewModel: ViewModel)
}
```

### Two-level Injection template
Generates an interface and an impl for two-level injection.

_Notes: Two-level injection is useful when you have both app-level and screen-level dependencies for a class whose dependencies you want to invert.
In such cases, you can avoid manually passing screen-level dependencies. 
There are two factories: a screen-level factory and an app-level factory, like this:_
```kotlin
interface FeatureViewModel {
    val state: FeatureState
    
    @Inject
    class Factory(
        private val coroutineScope: CoroutineScope,
        private val appLevelFactory: AppLevelFactory,
    ) {
        fun create(
            params: FeatureParams,
        ): FeatureViewModel = appLevelFactory.create(
            params = params,
            coroutineScope = coroutineScope
        )
    }
    
    interface AppLevelFactory {
        fun create(
            params: FeatureParams,
            coroutineScope: CoroutineScope,
        ): FeatureViewModel
    }
}
```

### Automation Templates
![Automation Templates](https://i.imgur.com/Vr0Gkrl.png)

Generate Robot, Semantics, and Verifier templates following our automation conventions. Available only under the `androidTest` source set.

---

## Module Analyzer
![module analyzer](https://i.imgur.com/kuYqEDV.png)

Right-click a module to analyze it using [DAGP](https://github.com/autonomousapps/dependency-analysis-gradle-plugin) and our internal scoring plugin (predicts JVM module compatibility).

The plugin helps invoke these tasks in your IDE's Run tab:
- **Android modules:** `./gradlew :module:analyzeModule`
- **Non-Android modules:** `./gradlew :module:projectHealth`

---

## build.gradle Actions
![build.gradle actions](https://i.imgur.com/MW2zART.png)

### Dependency Sorting
Right-click `build.gradle` to sort plugins and dependencies alphabetically.

### Test Fixtures Plugin
Right-click `build.gradle` to apply the Test Fixtures plugin and automatically create the required folders.

### Project Path Autocomplete
![auto-complete project path](https://i.imgur.com/IcXx7Rm.png)

Since we avoid Gradle [type-safe accessors](https://www.zacsweers.dev/dont-use-type-safe-project-accessors-with-kotlin-gradle-dsl/), this plugin provides autocomplete and validation for project paths. Invalid paths are highlighted with a red underline.

_Acknowledgments: The feature was adapted from [Slack foundry](https://github.com/slackhq/foundry/pull/1440)._

<!-- Plugin description end -->

---

License
-------

    Copyright 2026 BandLab Singapore Pte Ltd

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation