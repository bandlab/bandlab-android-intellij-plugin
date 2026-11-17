# IDE support for the strings compiler plugin: plan and prototype

Tracks [#271](https://github.com/bandlab/bandlab-android-intellij-plugin/issues/271). Compiler side:
[bandlab-localizer#334](https://github.com/bandlab/bandlab-localizer/pull/334) (design notes
`DESIGN-NOTES-333.md`, section 2.3 and 4). lab.fm is out of scope: it keeps the generated objects.

Status, 2026-11-17: prototype working in IntelliJ IDEA 2025.3.3 (K2) under the IntelliJ Platform
test framework. Android Studio is not installed here; Android Studio Panda 1 Patch 1 (2025.3.1.8,
build AI-253.29346.138) was fetched by the IntelliJ Platform Gradle plugin and used as a test IDE
where possible, see section 2.

## 1. How the compiler plugin gets into the IDE

The K2 IDE builds the compiler plugins of a project in `KtCompilerPluginsProviderIdeImpl`
(intellij-community, `plugins/kotlin/fir/src/.../fir/extensions/`, branch 253):

- It reads `pluginClasspaths` and `pluginOptions` from each module's Kotlin facet
  (`KotlinFacet...mergedCompilerArguments`), falling back to the project-wide Kotlin compiler
  settings. Gradle sync fills the facet from the compile task's arguments.
- Every `-Xplugin` jar goes through `substitutePluginJar`: first the extension point
  `org.jetbrains.kotlin.bundledFirCompilerPluginProvider`
  (`KotlinBundledFirCompilerPluginProvider.provideBundledPluginJar(project, jar)`); a jar no
  provider claims is dropped while the registry key `kotlin.k2.only.bundled.compiler.plugins.enabled`
  is true (the default).
- All substituted jars of all modules go into **one** `UrlClassLoader` per project whose parent is
  the Kotlin plugin's class loader, and the registrars are loaded with `ServiceLoaderLite` from that
  loader. The list is `distinct()`, so two user jars mapped to the same bundled jar load it once.
- `registerExtensions` runs per source module with that module's `-P` options, and the resulting
  `ExtensionStorage` is cached per `KaSourceModule` until a facet or compiler settings change.

The `-P` options reach the facet, including `tableFile` and `manifestDir`, which the localizer
Gradle plugin passes as `InternalSubpluginOption`s to keep them out of the task inputs: KGP's
`IdeCompilerArgumentsResolverImpl` builds the IDE arguments from `createCompilerArguments`, and
`CompilerPluginOptions.arguments` lists every option; `InternalSubpluginOption` is only skipped when
building task inputs (`KotlinGradleSubplugin.kt`).

**Decision: the bundled provider, not the registry key.** The prototype's
`TextResCompilerPluginProvider` claims the localizer's jar (its `CompilerPluginRegistrar` service
names `TextResCompilerPluginRegistrar`, read with the IDE's own `CompilerPluginRegistrarUtils`) and
the `text-keys` jar, and returns a jar shipped in this plugin's `compiler-plugins/` directory. The
registry key stays a fallback: with it off and no matching bundled build, the IDE loads the
project's own jar.

Correction to the earlier finding: Metro's IDE plugin does **not** use this extension point. Metro
asks users to turn the registry key off (`docs/installation.md`) and ships one compiler jar that
picks a compat implementation at runtime from `META-INF/compiler.version`, with an alias table for
Android Studio's versions (`compiler-compat/ide-mappings.txt`, e.g.
`2.3.255-dev-255=2.3.0-dev-9992`). Users of the provider are JetBrains' Compose plugin for Android
Studio (`plugin.k2.xml`), KMP-NativeCoroutines (returns `PathManager.getJarForClass(registrar)`)
and intellij-community's own bundled plugins.

Evidence (prototype test `TextResCompilerPluginIdeTest`): the module settings name a **dummy** user
jar that contains only the registrar service file, no classes; `Strings.save` and
`Strings.welcome(Strings.save)` still resolve and `Strings.nope` is the only error, so the classes
came from the bundled jar through the provider, with the registry key at its default.

## 2. Which jar: one per IDE compiler, not per Kotlin release

The IDE analyses with its own compiler build, and FIR APIs are not binary compatible across those
builds even at the same version number:

| IDE | IDE Kotlin (`META-INF/compiler.version`) |
|---|---|
| IntelliJ IDEA 2025.3.3 (target of this plugin) | `2.3.20-ij253-105` |
| Android Studio Panda 1 Patch 1 (2025.3.1.8) | `2.3.255-dev-255` (its `kotlin-plugin.jar` differs from IntelliJ's) |

A build of `:compiler-plugin` against the public `kotlin-compiler-embeddable:2.3.20` loads in
IntelliJ 2025.3.3, resolves properties, and fails on the first function member:
`NoSuchMethodError: ...createMemberFunction(...)Lorg/jetbrains/kotlin/fir/declarations/FirNamedFunction;`
— 2.3.20 renamed the return type, the `ij253` branch still returns `FirSimpleFunction`. The same
sources compiled against the IDE distribution's jars (`lib/*.jar`, `plugins/Kotlin/lib/*.jar`)
compile unchanged and pass every test. The jar contains no `com.intellij` references (checked with
`javap`), so the embeddable compiler's relocation of IntelliJ classes is not an issue either way.

Android Studio, run as a test IDE (`intellijPlatformTesting.testIde` with `androidStudio`
2025.3.1.8, light fixture, registry key off so the module's jar loads directly):

- the jar compiled against Studio's own jars (`-PlocalizerKotlinCompiler=2.3.255-dev-255`) resolves
  `Strings.save`, `Strings.welcome(Strings.save)`, `Plurals.songs(2)`; `Strings.nope` is the only
  unresolved reference; completion on `Strings.` offers `save`, `welcome`;
- the IntelliJ `2.3.20-ij253-105` jar gives the same result in Studio: both 253-line compilers keep
  `FirSimpleFunction`, so compatibility follows the IDE platform line more than the version string;
- the bundled provider itself picks `localizer-compiler-plugin-2.3.255-dev-255.jar` in Studio and
  the registrar loads through Studio's class loader and registers its FIR extension, but this
  plugin as a whole does **not** load in that test setup: its dependency on the Marketplace Android
  plugin (`platformPlugins`) shadows Studio's bundled one, which then fails on
  `org.jetbrains.idea.gradle.dsl`, so the EP registration was not exercised end to end in Studio.
  A Studio test task needs its own dependency set without that plugin.

So the unit of build is the IDE's compiler version. Runtime choice
(`pickCompilerPluginBuild`): the jar named after the IDE's `KotlinPluginLayout.ideCompilerVersion`
exactly; otherwise, for IntelliJ only, the newest build of the same `-ijNNN` branch whose build
number is not above the IDE's; Android Studio reports `2.x.255-dev-255` regardless of the dev
compiler it bundles, so it only gets an exact match. No match: no substitution, and the user is
back to the registry key.

**How the jars get built (coordination with the localizer).** The localizer already publishes
`compiler-plugin` per Kotlin version (`<version>-<kotlin>`) for Gradle. The IDE needs a different
axis, so:

1. Localizer: an IDE flavour of the existing scheme, compiled against an IDE distribution instead
   of `kotlin-compiler-embeddable`. The prototype adds `-PlocalizerIdeDistribution=<unpacked IDE>`
   to `compiler-plugin/build.gradle.kts` (in a copy, not the PR branch):
   `compileOnly(fileTree(dir) { include("lib/*.jar", "plugins/Kotlin/lib/*.jar") })`. Built with
   `-PlocalizerKotlinCompiler=2.3.20-ij253-105`, so the artifact version names the IDE compiler.
2. Package `text-keys` into the compiler plugin jar (shadow, no relocation needed). The IDE then
   sees one jar; today the provider also maps the separate `text-keys` jar to the bundled one.
3. Where the IDE distributions come from: the IntelliJ Platform Gradle plugin downloads exactly the
   IDEs this plugin supports (`intellijIdea(...)`, `androidStudio(...)`), so the IDE builds are
   cheapest **here**: a Gradle module per supported IDE that compiles the localizer's published
   `-sources.jar` (compiler-plugin and text-keys, version from the bandlab-android catalog)
   against that IDE and packages `compiler-plugins/localizer-compiler-plugin-<ide kotlin>.jar`.
   The localizer then only publishes sources jars; its CI keeps compiling against the next Kotlin.
   IntelliJ compilers are also on Maven (`kotlin-compiler-common-for-ide:2.3.20-ij253-NNN` in
   `intellij-dependencies`), Android Studio's are not, which rules out a Maven-only scheme.
4. The prototype vendors one prebuilt jar in `libs/compiler-plugins/` (like `libs/config.jar`),
   copied into the plugin's sandbox and zip by `prepareSandbox`, off the plugin class path.

## 3. Cache invalidation when strings.xml changes

- The compiler plugin's `Table` re-reads the file when its size or mtime changes, and its
  `ExtensionStorage` (and so the `Table`) is cached per module by the IDE (section 1).
- The generated members live in each module's LL FIR session. Sessions are dropped by
  `KotlinModificationEvent`s (`LLFirSessionCacheStorageInvalidator.invalidate`, Kotlin 2.3.0):
  `KotlinModuleOutOfBlockModificationEvent` drops the module and its transitive dependents,
  `KotlinGlobalSourceOutOfBlockModificationEvent` all source sessions.
- An XML edit does publish something: `FirIdeOutOfBlockPsiTreeChangePreprocessor` →
  `LLFirDeclarationModificationService` treats any non-Kotlin change as out-of-block **for the
  module owning the file**. That is `:common:android:strings`, which consumers do not depend on at
  compile time (the localizer trial makes it `runtimeOnly`; JVM modules never had it). An external
  write of a file without a document publishes a global module state event, but only for files in
  the project content and only when no document is loaded.
- **Hook:** `TextResTableFileListener`, a `BulkFileListener` (project listener). On any VFS event
  for a path that is a `tableFile` of some module, it publishes
  `publishGlobalSourceOutOfBlockModificationEvent()` (VFS `after` runs in a write action, which
  publishing requires) and restarts the daemon. One global event rather than per-module ones,
  since every Kotlin module of bandlab-android uses the common table. Covers a saved edit, the
  localizer CLI run by "Add string"/"Update" (`LocalizerRunner` refreshes the VFS after the
  process), VCS updates and branch switches. Unsaved edits are not seen until saved, because the
  compiler plugin reads the file from disk.
- Evidence: `testKeyAddedToStringsXmlResolvesWithoutSync` writes a new key to the file and expects
  `Strings.later` to resolve; with the publish line commented out it fails (`[later]` stays
  unresolved).

## 4. Completion without a sync

In the IDE the registrar declares every key (no source pre-scan), so completion on `Strings.`
lists the whole table (`testCompletionOffersEveryKeyOfTheTable`). After a key is added, section 3
makes it appear without a sync. A sync is needed once, to get the plugin's `-Xplugin` and `-P`
options into the facets; adding or removing keys never changes those options.

## 5. Navigation

- The members have no PSI. `TextResGotoDeclarationHandler` maps `Strings.save` to `<string
  name="save">` (`<plurals>` for `Plurals`) in the table's file
  (`testGoToDeclarationOpensTheStringsXmlEntry`). Member name equals key with the default
  `naming = ORIGINAL`, which bandlab-android uses; `CAMEL_CASE` would need the key mapping from
  `text-keys` (not done).
- Finding: K2's PSI `reference.resolve()` on a generated member returns the **package**
  (`PsiPackage`), and that element is invalid (`PsiInvalidElementAccessException` from
  `ResolveCache.ensureValidPsi`, unconditional) when no class declares the package in the module's
  scope. Any plugin calling `resolve()` on every reference would throw; this repo's Uikit icon line
  marker did in the first test run. In bandlab-android the table package holds `StringsTable`, so
  it is valid there; the fixture mirrors that.
- Tolgee: this plugin has no Tolgee navigation today (the Tolgee link is the CLI behind
  Add/Update). The manifest carries `api-url` and `project-id`, enough for an "Open in Tolgee"
  intention on a key; not built, see decisions.
- Find usages from a `<string>` to `Strings.<key>`: not done. Cheap route: a `ReferencesSearch`
  query executor for the `name` attribute of a table file entry that searches the word in Kotlin
  files (`PsiSearchHelper`) and keeps `Strings.<name>` references whose receiver is the table.

## 6. Quick fix and intentions

- Key detection (`textResKeyReference`): `Strings.<key>`, `Strings.<key>(args)`,
  `Plurals.<key>(count, ...)` where the receiver is a table object of the module's `-P table`
  options, identified by `ClassId`. Off the EDT the receiver is resolved with the Analysis API
  (`KaClassSymbol.classId`); on the EDT, where analysis is prohibited, from the import, a qualified
  receiver or the file's package. The member need not resolve, which is the "Add string" case.
- "Localizer: Add string" now targets the manifest entry whose base file is the table's
  `tableFile` (`targetsForBaseFile`), no `R`-class map involved (`testAddStringFixOffersAKeyMissing
  FromTheTable`). "Update" accepts `Strings.<key>`. The `R.string`/typealias detection stays for
  modules not migrated.

## 7. Risks

- **APIs used.** `KotlinBundledFirCompilerPluginProvider` is `@ApiStatus.Experimental` and
  `@ApiStatus.Internal` ("will most definitely be a subject of change"). `CompilerPluginRegistrarUtils`,
  `KotlinPluginLayout.ideCompilerVersion`, `KotlinFacet`, `KotlinCompilerSettingsTracker`,
  `KotlinFacetModificationTracker` are Kotlin-plugin implementation classes.
  `publishGlobalSourceOutOfBlockModificationEvent` is the Analysis API platform interface (no
  opt-in, but platform-side API). The plugin is distributed through Maven Central, not the
  Marketplace, so internal API use is a maintenance cost, not a publication blocker. `verifyPlugin`
  fails on internal API usages by default and its ignored-problems file does not cover them, so the
  PR drops `INTERNAL_API_USAGES` from `failureLevel` (still reported); compatibility problems per
  IDE still fail the build.
- **One jar per supported IDE compiler.** Every Android Studio or IntelliJ update that bundles a new
  Kotlin build needs a new jar here, or highlighting of `Strings` falls back to the registry key /
  red code. Same for compiler-plugin changes in the localizer: the IDE jars are rebuilt from the
  same sources. An Android Studio patch that changes its bundled compiler while still reporting
  `2.x.255-dev-255` would load an incompatible jar; keying by IDE build number as well closes that.
- **Failure mode of a wrong jar** is `NoSuchMethodError` inside FIR, logged as IDE errors on every
  highlighting pass, not a silent fallback. A smoke test per supported IDE is the guard.
- **Android Studio facet import with AGP built-in Kotlin** was not run: the KGP side is verified
  (section 1), Studio's import of those arguments for AGP 9 built-in Kotlin modules is not.
- **The localizer's `TABLE_USED_AS_VALUE` check fires in the editor** on an incomplete `Strings.`
  while typing (seen in Studio). Harmless, but the localizer could skip error-recovery qualifiers.
- **Testing like Metro.** Metro runs IDE Starter + Driver smoke tests (`ide-integration-tests`)
  over IntelliJ 2025.3.2 to 2026.3 and Android Studio Panda to Rabbit canaries, asserting
  highlights, inlays and no unresolved references, and the absence of plugin errors in `idea.log`;
  CI tests the newest IDE per branch on PRs and all of them on main. This plugin's equivalent: the
  light-fixture tests here per IDE (fast, catches API breaks), plus one Starter smoke test against
  Android Studio importing a tiny Gradle project with the localizer plugin.

## 8. Decisions for Andrey

1. Which Android Studio versions to support (one bundled jar each). Default taken: Panda (253),
   matching this plugin's `sinceBuild`, plus IntelliJ 2025.3 for this repo's tests; add the
   versions the team runs and the next canary.
2. Where IDE jars are built: here from the localizer's sources jars (recommended, section 2.3) or in
   the localizer repo against IDE distributions.
3. "Open in Tolgee" intention and find usages: worth building now?
4. `verifyPlugin` no longer fails on internal API usages (the provider EP is internal). Accept, or
   keep the provider in a separate optional plugin module with its own verification settings.

## 9. Prototype (this PR)

- `TextResCompilerPluginProvider` + `pickCompilerPluginBuild`: the bundled provider EP.
- `TextResTables`: tables and files from the module's facet `-P` options.
- `TextResKeys`: key detection; used by "Add string" and "Update".
- `TextResGotoDeclarationHandler`: `Strings.save` → strings.xml entry.
- `TextResTableFileListener`: the invalidation hook.
- `libs/compiler-plugins/localizer-compiler-plugin-{2.3.20-ij253-105,2.3.255-dev-255}.jar`:
  `:compiler-plugin` and `:text-keys` of the localizer branch (`feature/strings-compiler-plugin` at
  `9ded0df`), compiled against IntelliJ IDEA 2025.3.3's and Android Studio Panda 1 Patch 1's jars.
- Tests: 5 IDE fixture tests (resolution through the bundled jar, completion, go to declaration,
  Add string, a key added without a sync) and 4 unit tests (options parsing, jar choice).

Not run: this plugin end to end in Android Studio (see section 2 for what was), `runIde` against
bandlab-android (needs the localizer artifacts published and the trial branch synced), Studio's
facet import for AGP built-in Kotlin, K1 mode.
