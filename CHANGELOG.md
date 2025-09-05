<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# bandlab-android-intellij-plugin Changelog

## Unreleased

### Changed
- Fix submodules existence check to show error on dialog open (#122)

## [2.1.1] - 2025-09-01
### Changed
- Support for Groovy build scripts (#119)

## [2.1.0] - 2025-09-01
### Added
- Support project path auto-complete in build.gradle (#115)
- Added Params to activity template (#118)
- Rename vm -> viewModel in page template (#118)

### Changed
- Show error when the module root contains :api, :impl, :screen or :ui
- Check submodules existence eagerly

## [2.0.0] - 2025-08-12
### Added
- Rework Module Creation Dialog with CMP to align with convention (#106)
- Follow-up actions dialog after creating modules (#106)
- Support adding :impl and :screen to spotlight (#106)

### Changed
- Adjust activity template to private members injection, and make VM internal

## [1.8.0] - 2025-07-28
### Added
- Add support for spotlight all-projects file (#102)

## [1.7.7] - 2025-07-21
### Changed
- Update Activity, Page template to align with metro
- Adjust module creation flow with metro

## [1.7.5] - 2025-06-19
### Changed
- Update Page package
- Update config changes in manifest, we disabled all possible config changes in compose

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