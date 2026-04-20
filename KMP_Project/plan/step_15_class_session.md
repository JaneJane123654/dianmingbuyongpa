# Step 15: Translate ClassSessionManager and Feature-Level Monitoring Orchestration

## Objective

This step moves the higher-level monitoring or classroom-session manager into KMP. If Step 14 is the engine room, this step is the conductor. It should coordinate start and stop policy, permission or capability preconditions, feature-level user intents, repository writes for session lifecycle markers, and integration with the lower-level core session engine. The architecture maps session logic into `shared/feature/monitoring`, and the migration playbook explicitly lists `ClassSessionManager` after `CoreSessionManager`. This step should therefore build on the stable contracts already created, not redefine them.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 07 through 14
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/capability/PlatformCapabilities.kt`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/permission/PermissionGateway.kt`

Then inspect the legacy equivalents of:

- `ClassSessionManager`
- Any feature-level monitoring coordinator
- Any policy helpers that decide when monitoring may start, pause, or stop
- Any wrappers around permission, capability, or diagnostics checks

Read the legacy code line by line and separate feature policy from lower-level signal handling.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. What responsibilities belong to the higher session manager versus the core engine.
2. How start, stop, pause, resume, and reset actions are triggered.
3. Which permission and capability checks happen before entering a monitoring run.
4. How feature-level state is surfaced to the UI.
5. Which error, unsupported, or recovery branches are handled here instead of in the core engine.

Be explicit about every branch involving denied permissions, unsupported background monitoring, missing model selection, missing credentials, and restart after failure.

### Step 2: Translation

Implement the feature-level orchestration under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`

Expected outcomes:

- A `ClassSessionManager` or equivalent high-level feature coordinator.
- Clear start and stop commands that combine capability checks, permission checks, selected-model validation, and core-engine delegation.
- A feature-facing `UiState`-friendly projection or at least a stable feature state contract that presenters can consume later.
- Consistent error and unsupported-feature handling using the shared result and error model.

Do not build Compose presenters or screens in this step. This is still shared business logic.

### Step 3: Self-Check

Compare the new feature-level manager with the original one and state:

- Whether every precondition and recovery branch still exists.
- Whether permission or capability gating changed.
- Whether feature-state exposure to the UI lost any detail.
- Whether any classroom-specific policy was simplified and why.

## Implementation Instructions

1. Keep orchestration compositional. The high-level manager should delegate technical workflow to the core engine rather than duplicating it.
2. Make permission and capability decisions explicit so later UI can explain why a feature is unavailable.
3. Reuse repositories for config, model selection, and event history instead of reading raw storage directly.
4. Expose immutable state and one-off events through flows, not mutable listeners.
5. Avoid putting Compose-specific types or screen callbacks into `shared`.
6. Preserve the distinction between unsupported, denied, failed, and idle states if the old app treated them differently.
7. Update DI only for the manager and its direct dependencies.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- High-level monitoring session policy exists in `shared/commonMain`.
- Permission and capability checks are integrated.
- Feature-facing monitoring state is available for later presenters.
- The core engine and feature coordinator responsibilities are cleanly separated.
- UI steps can now bind to stable shared monitoring logic.

## Handoff Rule

Mark only Step 15 complete. Do not start Compose screen migration in the same turn. The next steps will map this shared feature logic into presenters and screens one feature at a time.
