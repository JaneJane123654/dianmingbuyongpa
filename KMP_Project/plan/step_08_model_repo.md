# Step 08: Translate Model Repository and Installation State Management

## Objective

This step migrates the logic that knows which AI models exist, which ones are available for the user to pick, which are already installed or downloaded, and which are still missing. In the architecture, this sits between local persistence and remote model catalog syncing. It should not yet fetch a remote catalog, but it must provide a stable local source of truth for selected model state, installation metadata, on-device availability, and provider routing hints. The old app likely spreads this logic across storage helpers, manager classes, and UI adapters. This step consolidates it into a proper repository layer in `shared/commonMain`.

## Required Reading Before Writing Code

Read these items first:

- Outputs from Steps 02, 05, 06, and 07
- The current SQLDelight schema and any model-installation local records
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/storage/PlatformDirectories.kt`

Then inspect the legacy equivalents of:

- `ModelRepository`
- `ModelDescriptor`
- Any manager that keeps track of installed models, selected model IDs, local model folders, version markers, or enable and disable flags

Read the code line by line. Pay close attention to how the old app decides whether a model is usable, stale, partially installed, remote-only, or unsupported on the current device.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which parts of model state come from persisted metadata versus filesystem inspection versus built-in defaults.
2. How the old code decides the “active” model.
3. Whether model selection depends on provider, locale, capability, or installation completeness.
4. How the old code handles missing directories, corrupt metadata, version drift, or duplicate model IDs.
5. Which branches are purely technical bookkeeping and which branches drive visible business behavior.

Be explicit about every business rule that could affect UI display, model routing, or install-state recovery.

### Step 2: Translation

Implement the repository and supporting mappers under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/model/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/` only if a tiny query or command use case is required for clarity

Expected outcomes:

- A repository interface and default implementation for model metadata and selection.
- Clear mapping from local persistence records into the `ModelDescriptor`-style entities created in Step 06.
- Stable handling of selected model ID, installation status, display ordering, and derived availability.
- Filesystem-aware checks routed through `PlatformDirectories`, not direct Java or Android file APIs inside `commonMain`.

Do not fetch remote provider catalogs yet. That belongs to Step 12.

### Step 3: Self-Check

Compare the new repository to the original logic and state:

- Whether all install-state branches still exist.
- Whether any filesystem check was simplified or deferred.
- Whether the selected-model fallback behavior remains equivalent.
- Whether all “missing”, “invalid”, “stale”, and “ready” branches are represented.

## Implementation Instructions

1. Keep the repository output domain-friendly. UI code should not depend on raw SQL rows or directory strings.
2. Separate pure mapping code from stateful repository methods so later tests are easy to write.
3. Preserve deterministic ordering if the old UI expects a stable model list.
4. Use `PlatformCapabilities` only if the legacy code truly gates model availability by platform support. If so, make the gating explicit and testable.
5. Avoid mixing remote sync concerns into this repository. Local truth first, remote enrichment later.
6. Translate callback-based model change listeners into `Flow` if the old code relied on them.
7. Update DI only for the repository and its immediate collaborators.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/model/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

Minor schema or local-record adjustments are allowed if Step 02 needs refinement to express the real model state accurately.

## Completion Checklist

- Local model metadata and selection are handled by a typed repository.
- Installation state is derived consistently.
- Filesystem-dependent logic is abstracted properly.
- No remote catalog sync logic was mixed into this step.
- Later service and UI steps can ask a single repository for model state.

## Handoff Rule

Mark only Step 08 complete when done. Do not move on to provider APIs or model catalog syncing in the same turn. This step must finish the local model truth before any network enrichment starts.
