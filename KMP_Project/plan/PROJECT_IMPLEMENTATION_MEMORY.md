# Cross-Platform Project Implementation AI Memory Document

## Highest-Priority Behavior Instruction

You are reading the canonical progress memory for this project. Your only job is to find the first unchecked item `[ ]` in the task table below, open only the referenced `plan/step_XX_*.md` file for that item, and complete exactly that one step. Do not do the next step. Do not “helpfully” bundle adjacent work. Do not skip ahead because you think later steps are easy. Complete one step only.

For the selected step, you must follow this mandatory three-pass workflow every single time:

1. **Step 1 - Analysis**  
   Read the corresponding legacy Java or Android source classes line by line as read-only reference material. In English, explain the class or classes’ core logic, edge cases, state transitions, and every business decision point that affects behavior.
2. **Step 2 - Translation**  
   Implement the KMP-equivalent code only inside `KMP_Project`. The new code must be functionally equivalent, aligned with the architecture, and limited to the write scope defined by the selected `plan/` file.
3. **Step 3 - Self-check**  
   Compare the new code against the original legacy logic. Explicitly state whether you simplified anything, whether all original branches are still covered, and whether any behavior had to be deferred.

Output contract for every turn:

- Perform only the first unchecked step.
- Modify or create code only under `KMP_Project`.
- Legacy project files outside `KMP_Project` may be read for comparison, but they are strictly read-only.
- After finishing the step, print the fully updated version of this memory document inside a fenced markdown code block and mark that one completed step as `[x]`.
- If the step is blocked by a genuine ambiguity that cannot be resolved safely, ask one concise blocking question and reprint this memory document unchanged. Do not mark the step complete.

## Global Project Summary

Project name: `KMP_Project`

Final goal: rebuild the old Android-first codebase as a Kotlin Multiplatform plus Compose Multiplatform application where:

- `shared` contains platform-agnostic business logic, repositories, domain services, use cases, and platform abstractions.
- `composeApp` contains shared Compose UI, presenters, navigation, app shell, and platform entry points for Android and iOS.
- `iosApp` remains a very thin wrapper that hosts the shared Compose application.

Primary success target: iOS-first maintainable architecture with Android as the second validation target. Web is out of scope.

Migration strategy: move stable primitives first, then repositories, then remote services, then session orchestration, then UI, then platform `actual` implementations, then hardening.

## Current Project Context Snapshot

This memory document was generated from the current `KMP_Project` scaffold already present in the repository. At the moment, the following baseline is known:

- Root modules already exist: `shared`, `composeApp`, and `iosApp`.
- `settings.gradle.kts` includes `:shared` and `:composeApp`.
- `shared/build.gradle.kts` already includes Coroutines, Serialization, Ktor, SQLDelight, Koin, and Multiplatform Settings.
- `composeApp/build.gradle.kts` already includes Compose Multiplatform, Decompose, Koin Compose, and Coil.
- Package roots already exist:
  - `com.classroomassistant.shared`
  - `com.classroomassistant.composeapp`
- Platform abstraction stubs already exist under:
  - `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/`
  - `shared/src/androidMain/kotlin/com/classroomassistant/shared/platform/`
  - `shared/src/iosMain/kotlin/com/classroomassistant/shared/platform/`
- A starter SQLDelight schema already exists at:
  - `shared/src/commonMain/sqldelight/com/classroomassistant/shared/db/AppDatabase.sq`
- A placeholder Compose app host already exists at:
  - `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/app/App.kt`
- Android and iOS entry points already exist at:
  - `composeApp/src/androidMain/kotlin/com/classroomassistant/composeapp/MainActivity.kt`
  - `composeApp/src/iosMain/kotlin/com/classroomassistant/composeapp/MainViewController.kt`

This means later implementation work must extend the scaffold rather than rebuild it from scratch.

## Global Development Rules and Shared Variables

These rules apply to every future implementation turn:

- You may write only inside `KMP_Project`. Do not modify legacy source files outside it.
- You may read legacy Java or Android files outside `KMP_Project` for translation reference only.
- `shared/commonMain` must never depend on Android SDK, UIKit, `Context`, `Activity`, `Handler`, `Looper`, `Toast`, `Intent`, `SharedPreferences`, or direct Java file APIs.
- All platform-specific behavior must flow through `expect/actual` or clearly separated host bridges.
- Shared business logic belongs in `shared`.
- Shared UI belongs in `composeApp`.
- `iosApp` must stay thin.
- Navigation must use `Decompose`.
- Networking must use `Ktor`.
- Serialization must use `kotlinx.serialization`.
- Local structured persistence must use `SQLDelight`.
- Key-value settings must use `Multiplatform Settings`.
- Dependency injection must use `Koin`.
- Image loading, if needed by shared UI, should use `Coil 3`.

Pinned toolchain and library versions from the current scaffold:

- Kotlin `2.2.20`
- Compose Multiplatform `1.10.3`
- Android Gradle Plugin `8.11.1`
- Coroutines `1.10.2`
- Serialization `1.9.0`
- Ktor `3.4.1`
- SQLDelight `2.3.2`
- Koin `4.1.1`
- Decompose `3.3.0`
- Multiplatform Settings `1.3.0`
- Coil `3.4.0`

Structural package intent:

- `shared/core`: low-level results, errors, utilities, cross-cutting contracts.
- `shared/data`: local data, remote APIs, repositories.
- `shared/domain`: domain models, services, use cases.
- `shared/feature`: feature-specific shared orchestration, especially monitoring and settings.
- `shared/platform`: `expect/actual` platform abstractions.
- `composeApp/app`: root app host and startup.
- `composeApp/navigation`: root navigation and destination composition.
- `composeApp/presenters`: UI-facing state adapters and intent handlers.
- `composeApp/screens`: pure Compose screens.
- `composeApp/designsystem`: shared visual primitives.

## Mandatory Translation Standards

For every selected step, preserve these conversion rules:

- Java POJOs become Kotlin `data class` or similarly explicit immutable models.
- Java enums become Kotlin enums or sealed hierarchies where business branching requires it.
- Callback interfaces become `suspend` functions, `StateFlow`, or `SharedFlow`.
- `ExecutorService`, raw threads, and handler coordination become coroutines with structured concurrency.
- Ad hoc listener lists become flow-based state or event streams.
- Raw JSON parsing or Gson patterns must be replaced with `kotlinx.serialization`.
- Android UI code in old Compose screens must be split into `UiState`, intent handling, and pure platform-neutral screens.
- Any logic that depends on permission or platform capability must remain explicit and user-visible.
- If behavior cannot be translated exactly, document the exact mismatch instead of hiding it.

## Completion and Handoff Protocol

When you finish the selected step:

1. Mark only that step as `[x]`.
2. Keep every later step as `[ ]`.
3. Reprint the entire updated memory document inside one fenced markdown code block.
4. Do not silently edit task wording, numbering, or order.
5. If you discover a missing prerequisite large enough to deserve a new step, stop and report it instead of folding it into the current step without telling the user.

## Single-Step Task Tracker

- [x] Step 01: Stabilize core result, error, dispatcher, and logging contracts. Target document: `plan/step_01_core_contracts.md`
- [x] Step 02: Build the local data foundation for settings, monitoring events, recordings, model installations, and answer history. Target document: `plan/step_02_local_data.md`
- [x] Step 03: Establish shared network, serialization, and DI infrastructure. Target document: `plan/step_03_network_di.md`
- [x] Step 04: Replace the placeholder app shell with a real root component and navigation skeleton. Target document: `plan/step_04_root_shell.md`
- [x] Step 05: Migrate default values, error codes, and session primitives. Target document: `plan/step_05_primitives.md`
- [ ] Step 06: Migrate user preferences, model descriptors, prompt templates, and LLM config entities. Target document: `plan/step_06_entities.md`
- [ ] Step 07: Translate config and preferences logic into typed settings repositories. Target document: `plan/step_07_settings_repo.md`
- [ ] Step 08: Translate model repository and installation state management. Target document: `plan/step_08_model_repo.md`
- [ ] Step 09: Translate recording repository and monitoring event persistence. Target document: `plan/step_09_recording_repo.md`
- [ ] Step 10: Translate LLM DTOs and the OpenAI-compatible API layer. Target document: `plan/step_10_llm_api.md`
- [ ] Step 11: Translate the app-facing LLM client and provider routing logic. Target document: `plan/step_11_llm_client.md`
- [ ] Step 12: Translate model catalog services and remote sync use cases. Target document: `plan/step_12_model_catalog.md`
- [ ] Step 13: Translate monitoring state models, events, and use-case contracts. Target document: `plan/step_13_monitoring_models.md`
- [ ] Step 14: Translate `CoreSessionManager` into coroutine-based shared logic. Target document: `plan/step_14_core_session.md`
- [ ] Step 15: Translate `ClassSessionManager` and feature-level monitoring orchestration. Target document: `plan/step_15_class_session.md`
- [ ] Step 16: Translate the settings feature into shared presenters, state, and Compose screen. Target document: `plan/step_16_settings_ui.md`
- [ ] Step 17: Translate the models feature into shared presenters, state, and Compose screen. Target document: `plan/step_17_models_ui.md`
- [ ] Step 18: Translate the monitoring feature UI and integrate it into the shared root. Target document: `plan/step_18_monitoring_ui.md`
- [ ] Step 19: Implement Android platform bridges for permissions, audio session, secure storage, and directories. Target document: `plan/step_19_android_bridges.md`
- [ ] Step 20: Implement iOS platform actuals for permissions, audio session, secure storage, and directories. Target document: `plan/step_20_ios_actuals.md`
- [ ] Step 21: Perform final hardening, diagnostics, and smoke verification. Target document: `plan/step_21_hardening.md`

## Recent Translation Notes

Step 01 mapped legacy `ExecutorService` or `ScheduledExecutorService` cancellation and `runOnMainThread` delivery into `AppDispatchers` plus an explicit `AppResult.Cancelled` branch so later coroutine migrations can preserve interruption semantics instead of collapsing them into generic failures.
Step 01 also normalized SLF4J class loggers and bracketed event markers such as `AI_SERVICE_FAILED` or `AI_RETRY` into `AppLogger` records with a stable `tag` and optional `eventCode`, leaving platform output implementations for later steps.
Step 02 split legacy `Properties` defaults and Java `Preferences` keys into typed `Multiplatform Settings` descriptors for hot-path key-value reads, while keeping `setting_entry` in SQLDelight as an explicit snapshot and migration rail instead of the primary runtime lookup path.
Step 02 also converted filesystem-only recording and model bookkeeping into structured rows with relative paths, status/source enums, and timestamp fields so later repositories can preserve legacy cleanup rules, custom URL installs, and corrupted-model detection without hardcoding Android or desktop file APIs.
Step 03 mapped the legacy split between long-running answer calls and short model-catalog fetches into reusable `RemoteHttpClientProfile` presets, so later Ktor services can preserve the original 20s/2-3min AI timeouts and 8s/12s catalog timeouts without instantiating ad hoc clients.
Step 03 also moved provider-specific header branching such as `Bearer`, `Token`, `x-api-key`, and `api-key` out of old OkHttp/HttpClient helpers into generic `RemoteRequestAuth`, `RemoteAuthContext`, and `RemoteAuthPlugin` scaffolding, leaving provider endpoint routing and retry decisions deferred to the later LLM service steps.
Step 04 mapped the legacy Android `LauncherActivity -> MainActivity` handoff and the desktop direct-to-main startup into one Decompose root stack that begins at `Launcher` and reserves explicit `Monitoring`, `Settings`, `Models`, and `Diagnostics` destinations without bundling their later feature logic early.
Step 04 also translated root startup failure handling and app-wide diagnostics/update/permission entry points into a guarded shared Koin bootstrap plus dedicated placeholder destinations, while intentionally deferring concrete permission bridges, crash-summary data, and update execution to the later platform and hardening steps.
Step 05 consolidated the duplicate legacy `ErrorCode` sources into one KMP enum that keeps the app-facing names (`CONFIG_INVALID`, `AUDIO_CAPTURE_FAILED`, `AI_SERVICE_FAILED`, etc.) while preserving the numeric code space and description mapping from the old core constants class via aliases and `descriptionForNumericCode`.
Step 05 also kept `SessionState` as the same flat four-state lifecycle enum and translated the `AudioConfig`, `VadDefaults`, and `AiDefaults` value objects as immutable common models with exact legacy defaults exposed through companion `Default` instances, while introducing `LlmProvider` only as the minimum typed replacement needed for `AiDefaults.providerDefault`.

## Final Reminder to the Next Implementation AI

Do exactly one step. Read the first unchecked task. Read its `plan/` file. Analyze the legacy source line by line. Translate only that scope into `KMP_Project`. Self-check against the original. Then reprint this memory document with exactly one additional checkbox marked complete.
