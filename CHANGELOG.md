<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# bandlab-android-intellij-plugin Changelog

## Unreleased

## [1.7.4] - 2025-05-08
### Changed
- Support module sorting for AudioStretch standalone and Edu app (#94)

## [1.7.3] - 2025-04-29
### Changed
- Rename CommonActivity2 to CommonActivity
- Update the module naming example to avoid confusion

## [1.7.2] - 2025-04-15
### Changed
- Adjust PageTemplateBuilder according to the latest ContributesComponent convention

## [1.7.1] - 2025-02-25
### Added
- Create a new PageTemplateCreateAction

### Changed
- Update activity template related to ContributesComponent changes

## [1.7.0] - 2025-02-06
### Added
- Create a new ActivityTemplateCreateAction

### Changed
- Update activity template to use ContributesComponent
- Rename MixEditorViewComponent to MixEditorComponent

## [1.6.3] - 2025-01-08
### Changed
- Update ComponentActivity.setContent import and add default insets type

## [1.6.2] - 2024-12-03
### Changed
- Fix AutomationTemplateCreateAction for new IDE version

## [1.6.1] - 2024-11-29
### Changed
- Fix SortDependenciesAction to support all module types (#80)

## [1.6.0] - 2024-11-08
### Added
- Introduce SortDependenciesAction

### Changed
- Module Creation: Prefill module path

## [1.5.1] - 2024-09-13
### Changed
- Adjust AutomationTemplate to avoid using context receiver

## [1.5.0] - 2024-08-20
### Added
- Added preferenceConfig plugin checkbox in the module creation flow (#70)
- Test fixtures support in the module creation flow + popup menu of build.gradle (#70)

## [1.4.2] - 2024-08-16
### Changed
- Do not specify until build to allow all future IDE versions

## [1.4.1] - 2024-07-10
### Changed
- Remove locale from configChanges (#64)

## [1.4.0] - 2024-06-26
### Added
- Automation: Create robot pattern template (#60)
- Marketplace: Add the plugin icon + update vendor name (#59)

### Changed
- Module convention update (#59)

## [1.3.0] - 2024-06-03
### Added
- Integrate plugin publishing lib (#53)