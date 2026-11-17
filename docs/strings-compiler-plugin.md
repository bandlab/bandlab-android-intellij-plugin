# IDE support for the strings compiler plugin

For [#271](https://github.com/bandlab/bandlab-android-intellij-plugin/issues/271); compiler side in
[bandlab-localizer#334](https://github.com/bandlab/bandlab-localizer/pull/334).

## Loading the compiler plugin

The K2 IDE loads `-Xplugin` jars from the module's Kotlin facet (`KtCompilerPluginsProviderIdeImpl`).
A jar is used only if a `bundledFirCompilerPluginProvider` substitutes it, or if the user turns off
the registry key `kotlin.k2.only.bundled.compiler.plugins.enabled`. We implement the provider, so
no registry key is needed. Metro takes the registry-key route instead.

Gradle sync puts the `-P` options into the facet, including the internal `tableFile`.

## One jar per IDE compiler

The bundled jar must be compiled against the IDE's own Kotlin, not the public release of the same
version. A build against public 2.3.20 fails in IntelliJ 2025.3.3 (`2.3.20-ij253-105`) with
`NoSuchMethodError` on `createMemberFunction`. The same sources compiled against the IDE's jars
work. Android Studio Panda reports `2.3.255-dev-255` and accepts both a jar built against its own
jars and the IntelliJ 253 one.

The provider picks a jar by the IDE's compiler version: an exact match, or the nearest earlier
build of the same `-ijNNN` branch. Studio's compiler isn't published to Maven, so
`updateLocalizerCompilerPlugins` (see `settings.gradle.kts`) compiles the localizer sources against
each supported IDE distribution: the latest stable and next Android Studio, plus the IntelliJ the
tests run on. Studio reports one version for a whole minor (Rabbit 2 and Rabbit 3 are both
`2.4.255-dev-255`); their jars are byte-identical today, and the task fails if that stops being
true.

## Invalidation

The compiler plugin re-reads strings.xml, but the IDE caches generated members per FIR session. An
XML edit only invalidates the module owning the file, which consumers don't depend on at compile
time. On any write to a table file, we publish a global source out-of-block event and restart
highlighting. Keys added by "Add string" or by hand resolve and complete without a sync.

## Navigation and find usages

Generated members have no PSI, so go to declaration maps `Strings.<key>` to its strings.xml entry.
Find usages uses that entry as the target from both sides, and a word search finds the
`Strings.<key>` references, since nothing indexes references to members without a declaration.
K2 resolves the member reference to its package, which is invalid when no class declares that
package. In bandlab-android, `StringsTable` declares it.

## Risks

- The provider extension point is `@ApiStatus.Internal`. `verifyPlugin` fails on any other internal
  API usage through its own check, since the verifier can't ignore one internal API.
- Every IDE update that changes the bundled Kotlin needs a new jar. A wrong jar fails with
  `NoSuchMethodError` in FIR rather than falling back. Metro guards this with IDE Starter smoke
  tests per IDE.
- Not verified: this plugin end to end in Android Studio, and Studio's facet import for AGP
  built-in Kotlin.
