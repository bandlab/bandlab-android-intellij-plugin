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
build of the same `-ijNNN` branch. Studio's compiler isn't published to Maven, so the jars are
compiled against IDE distributions. The localizer copy used here adds
`-PlocalizerIdeDistribution=<IDE dir>`.

## Invalidation

The compiler plugin re-reads strings.xml, but the IDE caches generated members per FIR session. An
XML edit only invalidates the module owning the file, which consumers don't depend on at compile
time. On any write to a table file, we publish a global source out-of-block event and restart
highlighting. Keys added by "Add string" or by hand resolve and complete without a sync.

## Navigation

Generated members have no PSI, so go to declaration maps `Strings.<key>` to its strings.xml entry.
K2 resolves the member reference to its package, which is invalid when no class declares that
package. In bandlab-android, `StringsTable` declares it.

## Risks

- The provider extension point is `@ApiStatus.Internal`, so `verifyPlugin` reports internal usages
  instead of failing on them.
- Every IDE update that changes the bundled Kotlin needs a new jar. A wrong jar fails with
  `NoSuchMethodError` in FIR rather than falling back. Metro guards this with IDE Starter smoke
  tests per IDE.
- Not verified: this plugin end to end in Android Studio, and Studio's facet import for AGP
  built-in Kotlin.
