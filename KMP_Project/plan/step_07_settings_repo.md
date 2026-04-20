# Step 07: Translate ConfigManager and Preferences Logic into Typed Settings Repositories

## Objective

This step is the first real repository translation. It turns the old app’s configuration managers and preference helpers into explicit KMP repositories backed by `Multiplatform Settings`, `SecureStore`, and the core or domain models created earlier. The architecture document is clear that `Properties`, `SharedPreferences`, and ad hoc file-backed config must not leak into `commonMain`. This step replaces those patterns with typed repositories that expose domain-friendly operations. It should cover both ordinary preferences and secure values, but it should not yet include model catalog network logic or session orchestration.

## Required Reading Before Writing Code

Read these files first:

- Outputs of Steps 01 through 06
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/secure/SecureStore.kt`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/storage/PlatformDirectories.kt`
- Any local data foundation files from Step 02

Then inspect the original legacy equivalents of:

- `ConfigManager`
- `PreferencesManager`
- Any helper that reads or writes API host config, selected provider, audio thresholds, user preferences, or secure API tokens

Read every branch carefully, especially where the old code merges defaults, upgrades missing values, or masks secrets.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which values are ordinary settings versus secure credentials.
2. Which settings are read often and therefore need efficient access.
3. Whether the old code exposes reactive listeners, polling, or one-shot reads.
4. Which defaults come from constants versus persisted user choices.
5. Any migration or fallback behavior for missing, malformed, or legacy keys.

You must identify every business rule that depends on a particular config field or security constraint.

### Step 2: Translation

Implement repositories under packages such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/settings/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/` only if a tiny settings-specific use case is necessary, though repository-first is preferred here

Expected outputs:

- A typed settings repository for ordinary user and app configuration.
- A secure settings or credentials repository that wraps `SecureStore` for tokens, secrets, and other sensitive values.
- Clear mapping between raw settings keys and the domain models from Step 06.
- Coroutine- or flow-based observation when the legacy behavior relied on listeners.

Do not implement model installation bookkeeping, remote model sync, or UI presenters in this step.

### Step 3: Self-Check

Compare the new repositories with the original managers and state:

- Whether every read and write path has an equivalent method.
- Whether any legacy listener behavior was simplified to one-shot calls.
- Whether secure values are now stored more safely than before, and whether that changes any calling semantics.
- Whether all fallback and default branches still exist and are testable.

## Implementation Instructions

1. Use typed methods. Avoid generic `getString(key)` style public APIs unless they are hidden behind a private adapter layer.
2. Keep secure and non-secure storage separate, even if the old app mixed them.
3. Favor constructor injection and interfaces so tests can replace the repositories later.
4. If the old code used callbacks or observers, translate them into `StateFlow`, `SharedFlow`, or suspend reads as appropriate.
5. Do not store rich history data in `Multiplatform Settings`; keep that separation intact for later database-backed repositories.
6. Reuse the core result and error contracts from Step 01 instead of throwing uncontrolled exceptions across the domain boundary.
7. Update DI registrations only for the settings-related repositories you introduce.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/settings/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- Ordinary app settings are accessible through a typed repository.
- Secure credentials are accessible through a dedicated secure repository.
- Defaults and fallback logic are explicit.
- Listener-style legacy behavior is mapped to coroutines or flows where needed.
- No Android `SharedPreferences` or Java `Properties` APIs appear in `commonMain`.

## Handoff Rule

Mark only Step 07 complete when finished. Do not start model installation or LLM client work in the same turn. Those are separate repository and service migrations that depend on this configuration layer being settled.
