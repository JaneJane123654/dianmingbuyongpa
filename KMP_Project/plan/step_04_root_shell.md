# Step 04: Replace the Placeholder App Shell with a Real Root Component and Navigation Skeleton

## Objective

The current `composeApp` module proves that the project builds, but it still uses a placeholder `ClassroomAssistantApp()` that renders a single text message. The architecture explicitly calls for a Decompose-driven root component, a shared navigation stack, and Compose entry points for Android and iOS that mount the same application tree. This step upgrades the shell from “the framework boots” to “the app has a real root structure.” It must stay intentionally feature-light: no full settings screen, no monitoring workflow, and no model management yet. The goal is to give later UI steps a stable navigation host and dependency bootstrapping point.

## Required Reading Before Writing Code

Read these files first:

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`
- `composeApp/build.gradle.kts`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/app/App.kt`
- `composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/MainActivity.kt`
- `composeApp/src/iosMain/kotlin/com/classroomassistant/composeapp/MainViewController.kt`
- DI and core foundation files created in Steps 01 through 03

Then inspect the old app’s main activity, root compose host, navigation graph, and any launcher or home screen controllers. Read them line by line. Your job is not to migrate every screen yet, but you must understand how the old app enters the monitoring flow, settings flow, and model flow.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the legacy app decides which top-level screen to show.
2. Whether navigation is purely route-based, state-based, or driven by side effects.
3. Which root-level dependencies are created at app startup.
4. Whether there are app-wide banners, permission prompts, or diagnostics hooks visible from the root.
5. What lifecycle assumptions the old app makes that Decompose must preserve.

Call out every edge case where the old app restores state, handles back navigation, or conditionally hides a feature based on capability or configuration.

### Step 2: Translation

Implement the root shell under paths such as:

- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/app/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/navigation/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/designsystem/`

Expected outcomes:

- `ClassroomAssistantApp()` becomes a real app host rather than a placeholder text.
- A `RootComponent` or equivalent Decompose root component exists, with route or config child definitions for at least launcher, monitoring, settings, models, and diagnostics placeholders.
- Android and iOS entry points both mount the same shared app host.
- Koin startup or equivalent DI bootstrap happens exactly once in a predictable place.
- Placeholder screens can stay visually simple, but the navigation skeleton must be real and future-proof.

Do not build full feature presenters or migrate detailed business logic in this step. Placeholder state and screens are acceptable so long as the root architecture is real.

### Step 3: Self-Check

Compare the new shell with the legacy root flow and note:

- Whether any startup branch was deferred.
- Whether state restoration or back handling behavior changed.
- Whether you introduced any navigation simplification that later steps must revisit.
- Whether all intended top-level destinations now have a reserved place in the navigation tree.

## Implementation Instructions

1. Use `Decompose 3.3.0`, matching both the architecture choice and the version in `libs.versions.toml`.
2. Keep navigation definitions explicit and serializable where practical. Avoid opaque string-only routing if a small config sealed type is clearer.
3. The app shell should depend only on stable infrastructure already created. If a feature is not implemented yet, use placeholder components or stub `UiState`.
4. Respect the package structure proposed in the architecture: `app`, `navigation`, `designsystem`, `screens`, `presenters`.
5. Keep root-level visuals intentionally minimal. The purpose is architecture, not polish.
6. Do not introduce Android-specific APIs like `LocalContext`, `NavController`, or `rememberLauncherForActivityResult` into `commonMain`.
7. Make sure the shell remains compatible with the thin `iosApp` wrapper strategy described in `iosApp/README.md`.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/`
- `KMP_Project/composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/`
- `KMP_Project/composeApp/src/iosMain/kotlin/com/classroomassistant/composeapp/`

You may touch `shared` only if a small interface or DI entry point must be exposed for app startup.

## Completion Checklist

- The app has a real shared root component.
- The navigation skeleton covers the future feature areas.
- Android and iOS use the same shared app host.
- DI starts in a deterministic way.
- No feature implementation was accidentally bundled into the shell step.

## Handoff Rule

When complete, mark only Step 04 as done. Do not implement real settings, models, or monitoring screens in the same turn. Those need their own isolated plan files.
