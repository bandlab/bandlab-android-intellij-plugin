# bandlab-android-intellij-plugin

<!-- Plugin description -->
## Create modules with BandLab Android convention
- Select Android/ Kotlin module
- Select plugins that you want to apply
- Opt-in the compose convention for new features
- Generate Dagger module and expose to App or MixEditor
- Sort dependencies automatically

![Module Creation Wizard](https://i.imgur.com/bpgcbfb.png)

## Create Screen templates
- Create an Activity, ViewModel and Manifest according to the latest screen building convention.
- Create a Page, ViewModel with preferred injection strategy.

![Screen Template](https://i.imgur.com/Va9QFkb.png)

![Page Template](https://i.imgur.com/7LmvABk.png)

## Create Automation file templates
Create Robot, Semantics and Verifier templates to align with our automation test convention.

![Automation Template](https://i.imgur.com/upwfk3R.png)

## Actions for build.gradle files
![Automation Template](https://i.imgur.com/wMOB46s.png)

### Sort dependencies
Sort plugins and dependencies alphabetically, the option is only available when right-clicking on build.gradle.

### Apply Test Fixtures plugin
Apply the test fixtures plugin and create the required folder, the option is only available when right-clicking on build.gradle.

<!-- Plugin description end -->

## Installation

Add the following URL to [your plugin configuration](https://www.jetbrains.com/help/idea/managing-plugins.html#add_plugin_repos) 
and search for "BandLab", you'll see our shiny plugin!
```
https://artifactory.bandlab.io/artifactory/intellij-idea-plugins/updatePlugins.xml
```

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
