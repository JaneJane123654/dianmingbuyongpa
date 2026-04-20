# Step 01: Stabilize Core Contracts and Migration Guardrails

## Objective

This step creates the non-negotiable base contracts that every later migration step will depend on. The current `KMP_Project` scaffold already contains module wiring, platform `expect/actual` placeholders, a minimal `sharedModules()` registration point, and a shell Compose entry point. What it does not yet contain is a stable common vocabulary for results, errors, dispatchers, and logging. Do not skip this. If later repository, service, or UI steps invent their own result wrappers or coroutine rules, the migration will drift quickly and the AI will start producing inconsistent code. The purpose of this step is to make `shared/commonMain` opinionated enough that every subsequent class translation has an obvious home and a predictable contract style.

## Required Reading Before Writing Code

Read these files first inside `KMP_Project`:

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/di/SharedModules.kt`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/permission/PermissionGateway.kt`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/audio/PlatformAudioSessionController.kt`

Then inspect the original legacy Java/Kotlin classes that define any existing app-wide result wrapper, error code model, logger abstraction, executor or handler utilities, or thread coordination helpers. The legacy files are read-only reference material. You may analyze them, but all created or modified files must stay inside `KMP_Project`.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Read the legacy core helper classes line by line and explain in English what each one is responsible for. Your explanation must cover:

1. How success and failure are represented today.
2. Whether errors carry codes, messages, causes, retry hints, or user-facing copy.
3. How background work is scheduled today and where callbacks or listeners are used.
4. How logging is invoked, and whether log messages have stable tags or categories.
5. Any edge case where the old code treats cancellation, missing config, unsupported platform features, or unexpected null values specially.

Do not summarize loosely. Explicitly call out every business decision that later migrations must preserve.

### Step 2: Translation

Implement only the common foundational contracts. Create or update files under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/core/result/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/core/error/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/core/util/`

The recommended outcome is a small, stable set of primitives:

- An `AppResult` sealed type, or an equivalent mature wrapper that clearly models success and failure.
- A base `AppError` hierarchy that later repositories and services can reuse.
- A coroutine dispatcher abstraction such as `AppDispatchers` or `DispatcherProvider`.
- A lightweight `AppLogger` interface that preserves logging semantics without binding `commonMain` to Android or iOS APIs.

Keep the API surface small. This step is not allowed to introduce repository logic, network calls, screen state, or platform-specific logging implementations beyond interfaces and safe no-op defaults.

### Step 3: Self-Check

Compare the new KMP core contracts with the original legacy classes. State clearly:

- Whether you simplified any exception or error mapping logic.
- Whether cancellation behavior changed.
- Whether any callback-style completion signal was converted into `suspend` or `Flow`.
- Whether every original branch still has an equivalent representation in the new contracts.

If something could not be translated one-to-one, document the exact reason instead of hand-waving.

## Implementation Instructions

1. Keep everything in `commonMain`. Do not add Android or iOS implementations in this step unless a file cannot compile without a trivial stub.
2. Prefer sealed interfaces, data classes, and constructor injection. Avoid singleton state unless it is purely stateless utility code.
3. Keep naming aligned with the documented architecture package layout under `core/model`, `core/result`, `core/error`, and `core/util`.
4. Use `kotlinx.coroutines` abstractions already available in `shared/build.gradle.kts`. Do not introduce a new concurrency library.
5. Preserve room for domain-specific error codes later. This step can define the envelope; Step 05 will add concrete migration of legacy `ErrorCode` style enums and defaults.
6. Update `SharedModules.kt` only if the new foundational interfaces need safe default bindings. If you do, keep the module list readable and avoid registering unfinished business services.
7. Add concise comments only where the contract would otherwise be ambiguous to the next AI worker.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/core/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

You may update imports in existing `shared` files if required for compilation. Do not edit `composeApp` yet.

## Completion Checklist

- `shared/commonMain` has a single, reusable result contract.
- `shared/commonMain` has a reusable base error contract.
- Coroutine dispatching is abstracted away from raw `Dispatchers` calls where appropriate.
- Logging is represented by an interface, not by Android APIs.
- No file in this step references `Context`, `Activity`, `Handler`, `Looper`, or iOS/UIKit classes.
- The step is small enough that later repositories and services can depend on it without redefining their own wrappers.

## Handoff Rule

When this step is complete, mark only Step 01 as done in the project memory document. Do not start database setup, DTO migration, or UI work in the same turn. The whole point of this step is to freeze the shared language of the codebase before any domain translation begins.
