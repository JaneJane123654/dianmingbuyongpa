# Step 19: Implement Android Platform Bridges for Permissions, Audio Session, Secure Storage, and Directory Access

## Objective

This step fills in the Android side of the platform abstractions that the shared layers have been targeting since the beginning. The scaffold already contains Android `actual` placeholder files for permissions, capabilities, secure storage, audio session control, and platform directories. The migration playbook explicitly says platform `actual` work should come after shared logic is in place, and the current project follows that pattern. This step must therefore translate the old Android platform glue into clean `shared/androidMain` implementations plus any minimal `composeApp/androidMain` bridge code needed to connect runtime permission flows or lifecycle entry points.

## Required Reading Before Writing Code

Read these files first:

- Existing Android `actual` placeholders under `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/`
- `composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/MainActivity.kt`
- Outputs from Steps 04, 07, 15, and 18
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the original Android platform code line by line, especially:

- Runtime permission handling
- Audio session or microphone-mode setup
- Secure credential storage or encrypted preferences
- Directory or file-path helpers
- Any notifications or app-settings jump helpers related to monitoring

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the old Android app checks and requests permissions.
2. How the old app configures audio behavior before, during, and after monitoring or recording.
3. How secure values are stored and whether migration from older storage exists.
4. Which directories are required for cache, models, and recordings.
5. Which branches handle denied permissions, permanently denied permissions, missing directories, unsupported hardware, or background-monitoring constraints.

List every business-relevant branch, especially those that affect feature availability or user messaging.

### Step 2: Translation

Implement Android-specific code under paths such as:

- `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/permission/`
- `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/audio/`
- `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/secure/`
- `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/storage/`
- `composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/` for any minimal permission coordinator or activity bridge

Expected outcomes:

- Real Android permission status and request handling that maps correctly to the shared `PermissionStatus` contract.
- Real audio-session or audio-mode behavior aligned with the old app’s monitoring needs.
- Real secure-storage integration for credentials.
- Real directory resolution for cache, models, and recordings.
- Minimal Android bridge code to connect UI intent flows to runtime permission APIs where necessary.

Do not rewrite shared business logic in Android code. This step is about filling the `actual` side of abstractions and host-only glue.

### Step 3: Self-Check

Compare the new Android platform implementation with the old Android code and state:

- Whether all permission states, including permanently denied or settings-required flows, are represented.
- Whether audio-session transitions preserve the old behavior.
- Whether secure storage and directory paths are equivalent or intentionally improved.
- Whether any Android-only branch was deferred and documented clearly.

## Implementation Instructions

1. Keep all feature-independent platform behavior in `shared/androidMain` actuals.
2. Use `composeApp/androidMain` only for UI-host concerns that cannot live in `shared`, such as activity-backed permission launching.
3. Do not let Android types leak back into `commonMain`.
4. Keep permission flows testable and explicit. If you need a coordinator, make the contract easy to follow.
5. Preserve the capability assumptions already encoded in `PlatformCapabilities.android.kt`.
6. If the old app had notification helpers that monitoring depends on, implement only the minimal hooks needed by existing shared abstractions.
7. Avoid overengineering. This step should fill the existing seams, not create a second Android architecture alongside the KMP one.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/`
- `KMP_Project/composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/`
- `KMP_Project/composeApp/src/androidMain/AndroidManifest.xml` if Android permission declarations or activity wiring must be aligned

## Completion Checklist

- Android permission actuals are real.
- Android audio-session actuals are real.
- Android secure-store and directory actuals are real.
- Any minimal Android host bridge required by the shared UI is in place.
- Shared code remains free of Android platform classes.

## Handoff Rule

Mark only Step 19 complete. Do not implement iOS actuals or final smoke checks in the same turn. Android host work needs to be reviewed separately before moving to the iOS side.
