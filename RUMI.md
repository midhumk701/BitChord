# Rumi — Android preview

Rumi is a modified BitChord fork by midhumk701. Initial UI and identity changes: 2026-11-17.

This preview introduces a charcoal/lavender theme, system sans-serif typography, an original headphone-cat adaptive icon, and the Rumi home header with the existing Listen now heading. It retains BitChord's real library, feed, playback controls, floating navigation and full-bleed artwork player. The concept's sample artwork and fictional playlists are not shipped as real content; actual music artwork comes from the selected tracks. This first implementation is not a pixel-identical recreation of the concept.

Android IDs: `io.github.midhumk701.rumi` (production), `io.github.midhumk701.rumi.dev` (preview). Code namespaces remain upstream-compatible. The updater now checks this fork's releases, not the upstream app's releases.

## Try it

The `Rumi preview APK` workflow on the `rumi-branding` branch runs Android unit tests and builds debug-signed APKs. Download `Rumi-preview-APKs` from a successful workflow run; the universal APK works across the supported architectures. This preview is for testing, not a production release. Ephemeral CI debug keys may require uninstalling a prior preview before installing a new one, deleting that preview's app data. Public releases need a separately secured, stable release signing key.

Local build requires JDK 17, Android SDK platform 37, CMake 3.22.1, and the project's native dependencies:

```sh
bash ./gradlew :app:testDevDebugUnitTest :app:assembleDevDebug -Pbitchord.bundleWear=false
```

The preview omits the bundled watch APK. Some upstream integrations depend on the original app's services or signing identity and have not been verified for this fork. Search, playback, background playback, library persistence and layout must be checked on a real Android device before release.

## Attribution and source

Based on https://github.com/kushagrasinghx/BitChord. Original copyright and licence notices remain in place. Rumi modifications are distributed under the same GPLv3 terms; see LICENSE. Publish the corresponding source revision and required build files alongside any distributed APK. Source: https://github.com/midhumk701/BitChord/tree/rumi-branding
