# M0G

M0G is a private, on-device facial geometry visualizer for Android. It uses ML Kit face landmarks and CameraX to produce an animated camera experience and a repeatable 12-zone bone-map proxy report.

## What the points mean

The score is **not** an attractiveness, health, race, identity, or medical score. A phone camera cannot see literal bones, bone density, or complete 3D anatomy. M0G reports anatomy-inspired 2D geometry proxies based on landmark visibility, approximate proportions, left/right alignment, and head angle. The named zones are: frontal bone, brow ridge, glabella, nasal bones, both orbital regions, both zygomatic regions, maxilla, both mandibular sides, and menton/chin.

All processing is intended to happen on the device. No account, upload, or face recognition is implemented.

## Build

Requires JDK 17, Android SDK platform 34, and Gradle. Run:

```bash
./gradlew :app:assembleRelease
```

The release artifact is `app/build/outputs/apk/release/app-release-unsigned.apk`. A GitHub Actions workflow builds it on pushes to `v*` tags or through **Actions → Android release → Run workflow**. The current release is unsigned so it can be verified and then signed with the distributor's private keystore.

## Local setup

The Arena sandbox used to create this project did not contain a JDK or Android SDK, so compilation must run on a configured Android machine or GitHub Actions runner.
