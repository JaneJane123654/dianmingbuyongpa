# Step 20: Implement iOS Platform Actuals for Permissions, Audio Session, Secure Storage, and Directory Access

## Objective

This step completes the iOS side of the platform abstraction layer. The architecture makes iOS the first-priority runtime target and repeatedly warns that iOS background behavior, permission APIs, and audio constraints differ meaningfully from Android. The current scaffold already includes iOS `actual` placeholders for permissions, capabilities, secure storage, audio session control, and directory access. This step fills those placeholders with real Foundation, AVFoundation, Security, and related integrations while preserving the capability-gating strategy described in the architecture. The result should be an honest iOS implementation, not a fake parity layer that claims unsupported capabilities work.

## Required Reading Before Writing Code

Read these files first:

- Existing iOS `actual` placeholders under `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/capability/PlatformCapabilities.kt`
- `composeApp/src/iosMain/kotlin/com/classroomassistant/composeapp/MainViewController.kt`
- Outputs from Steps 04, 07, 15, and 18
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the original iOS-related glue if any exists, plus any old Android logic that the architecture explicitly says must be downgraded or capability-gated on iOS. Read line by line and note every place where iOS must behave differently.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the old project expects microphone, speech-recognition, and notification permissions to work.
2. Which audio-session transitions are required for foreground monitoring and recording.
3. How secure values should be stored on iOS and whether migration from a less secure store matters.
4. Which directories are required for application support, cache, models, and recordings.
5. Which features are intentionally unsupported or downgraded on iOS, especially around continuous background monitoring.

List every business-relevant branch, including denied, restricted, not-determined, and unsupported states.

### Step 2: Translation

Implement iOS-specific code under paths such as:

- `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/permission/`
- `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/audio/`
- `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/secure/`
- `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/storage/`
- `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/capability/`

Expected outcomes:

- Real iOS permission status and request handling.
- Real iOS audio-session management for supported foreground scenarios.
- Real secure-storage integration backed by iOS-native secure storage primitives.
- Real directory resolution for app support, cache, models, and recordings.
- Honest capability reporting that never pretends unsupported Android-style behavior works on iOS.

Do not move business logic into Swift or the Xcode shell. Keep Swift-side changes minimal unless a tiny entry-point glue change is unavoidable.

### Step 3: Self-Check

Compare the new iOS implementation with the architecture rules and any legacy expectations:

- Whether all permission states are represented accurately.
- Whether unsupported capabilities are reported as unsupported instead of silently faked.
- Whether audio-session behavior matches the intended foreground-first design.
- Whether any iOS-specific limitation requires later product or UX handling and has been documented clearly.

## Implementation Instructions

1. Prefer Foundation, AVFoundation, UserNotifications, and Security APIs as appropriate to the abstraction being filled.
2. Preserve `PlatformCapabilities.ios.kt` as the source of truth for capability honesty. Do not quietly broaden support claims unless the code genuinely supports them.
3. Keep all platform names and native APIs trapped in `iosMain`.
4. Make permission methods deterministic and explicit. Unsupported capabilities should return `Unsupported`, not ambiguous denial.
5. Do not route iOS business behavior through `iosApp` unless the KMP boundary truly cannot express it.
6. If the old Android product behavior cannot exist on iOS, keep the downgrade explicit and let the shared UI react through capability flags.
7. Keep the host glue thin so `MainViewController()` remains the main mounting point.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/`
- `KMP_Project/composeApp/src/iosMain/kotlin/com/classroomassistant/composeapp/` if a tiny host integration change is required
- `KMP_Project/iosApp/` only if an unavoidable wrapper adjustment is needed, and only as a minimal shell change

## Completion Checklist

- iOS permission actuals are real.
- iOS audio-session actuals are real for supported scenarios.
- iOS secure-store and directory actuals are real.
- Capability reporting remains honest.
- No unsupported Android behavior is silently faked on iOS.

## Handoff Rule

Mark only Step 20 complete. Do not jump into final hardening in the same turn. The iOS platform layer should be reviewed independently because it carries the highest platform-risk surface.
