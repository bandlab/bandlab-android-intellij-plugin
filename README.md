# bandlab-android-intellij-plugin

<!-- Plugin description -->
## Create modules with BandLab Android convention
- Allow only api, impl, ui, screen name for modules
- Support creating multiple modules at once
- Contextual Module path pre-fill and autocomplete
- You can optionally expose impl and screen to AppGraph or MixEditorGraph, AppGraph is selected by default
- Activity and Page template options
- Module existence check per submodules, ex. check :foo:api and :foo:impl existence separately
- Support adding to spotlight after module creation

![Module Creation Wizard](https://i.imgur.com/nD1wqPw.png)

## Create Screen templates
- Create an Activity, ViewModel and Manifest according to the latest screen building convention.
- Create a Page, ViewModel with preferred injection strategy.

![Screen Template](https://i.imgur.com/Va9QFkb.png)

## Create 2-level Injection template
Provide a template for 2-level injection. See more details here:
https://bandlab.atlassian.net/wiki/spaces/Android/pages/3294724236/Dependency+Injection#Two-level-Injection

## Create Automation file templates
Create Robot, Semantics and Verifier templates to align with our automation test convention.

![Automation Template](https://i.imgur.com/upwfk3R.png)

## Module Analyzer
![module analyzer](https://i.imgur.com/kuYqEDV.png)

Available upon right-clicking on a module. The action executes tasks from [DAGP](https://github.com/autonomousapps/dependency-analysis-gradle-plugin), 
and our internal plugin to score an Android module for how likely it can be converted to a JVM module.

The action starts a terminal window, and execute the following tasks based on the module type:
- Android module: `./gradlew :module:analyzeModule`
- Non-android module: `./gradlew module:projectHealth`

## Actions for build.gradle files

### Auto Complete project path
![auto-complete project path](https://i.imgur.com/ED5Lslh.png)

We do not use Gradle type-safe accessors due to the reason [here](https://www.zacsweers.dev/dont-use-type-safe-project-accessors-with-kotlin-gradle-dsl/), so we implemented the auto-complete for project path.
Project paths will also be validated, if you see your dependency has a red underline, it means it's invalid path.


### Sort dependencies
![build.gradle actions](https://i.imgur.com/wMOB46s.png)

Sort plugins and dependencies alphabetically, the option is only available when right-clicking on build.gradle.

### Apply Test Fixtures plugin
Apply the test fixtures plugin and create the required folder, the option is only available when right-clicking on build.gradle.

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
