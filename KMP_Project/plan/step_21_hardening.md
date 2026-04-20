# Step 21: Final Hardening, Diagnostics, and Smoke Verification

## Objective

This final step is the controlled landing pass. By the time it begins, the project should already have shared business logic, shared feature screens, and real Android and iOS platform actuals. What remains is not another large translation batch, but a focused verification and hardening pass that makes the migrated KMP app safer to continue evolving. The architecture’s milestone section calls out diagnostics, permission guidance, error recovery, and experience alignment as the final stage. This step should therefore clean up rough edges, add any missing lightweight diagnostics surfaces, verify DI and navigation integration, and run smoke-level checks without reopening major architectural decisions.

## Required Reading Before Writing Code

Read these files first:

- Outputs from all prior steps, especially 15 through 20
- `docs/ARCHITECTURE.md`, milestone section
- `docs/MIGRATION_PLAYBOOK.md`, acceptance criteria section
- Current root navigation and feature entry points
- Current platform actual implementations and DI modules

Then inspect any legacy diagnostics screen, log viewer, permission guidance UI, or recovery flows. Read line by line and identify what still matters in the new architecture versus what can stay intentionally simplified.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which user-visible rough edges remain after the main migration.
2. Which error states currently lack clear recovery messaging.
3. Whether any permission or capability blocker is technically handled but still unclear in the UI.
4. Which lightweight diagnostics are necessary for debugging model state, monitoring state, or credential issues.
5. Which smoke checks are required to consider the migrated step sequence coherent on Android and iOS.

List every high-risk gap that could make the project feel “technically migrated but operationally fragile.”

### Step 2: Translation

Implement only the minimum hardening required under paths such as:

- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/diagnostics/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/`
- Any test source sets already present in `shared` or `composeApp` if smoke tests are practical

Expected outcomes:

- Clearer recovery and unsupported-state messaging where the migrated app needs it.
- Any lightweight diagnostics screen or panel needed by the architecture.
- Final DI, navigation, and feature wiring cleanup.
- Smoke-level verification steps for shared logic, repository initialization, and platform host startup.

Do not restart large feature implementation work here. This step is for stabilization only.

### Step 3: Self-Check

Compare the final KMP state with the legacy app and the architecture goals:

- Whether any major business branch is still unimplemented.
- Whether any platform-specific limitation is clearly surfaced to the user.
- Whether diagnostics and error recovery are now good enough for continued iterative development.
- Whether any simplification remains and what exact risks it creates.

## Implementation Instructions

1. Prioritize fixes that reduce ambiguity and operational fragility, not cosmetic polish for its own sake.
2. Keep diagnostics simple, factual, and cross-platform where possible.
3. Verify that `commonMain` still stays free of Android and iOS classes after the full migration.
4. Favor smoke tests that validate repository initialization, flow wiring, and platform-gated feature entry over brittle UI snapshot tests.
5. If a diagnostics screen is added, keep it accessible from the root shell but clearly secondary to the main user flow.
6. Document any remaining known limitation rather than hiding it.
7. Do not silently expand scope into new features. Finish the migration cleanly.

## Allowed Output Scope

You may write inside:

- `KMP_Project/shared/`
- `KMP_Project/composeApp/`
- `KMP_Project/iosApp/` only if a tiny host tweak is required for smoke completeness

All edits must still be tied directly to hardening, diagnostics, or verification.

## Completion Checklist

- Error and unsupported states have clear handling.
- DI and navigation startup paths are coherent.
- Lightweight diagnostics or recovery support exists where needed.
- Smoke verification has been performed or the exact blocker has been documented.
- The project is ready for iterative feature completion beyond the initial migration wave.

## Handoff Rule

Mark only Step 21 complete when done. If you discover a missing prerequisite so large that it deserves its own migration step, stop and report it instead of hiding it inside the hardening pass.
