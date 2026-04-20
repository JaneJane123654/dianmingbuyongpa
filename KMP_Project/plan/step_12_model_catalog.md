# Step 12: Translate Model Catalog Services and Remote Sync Use Cases

## Objective

This step migrates the logic that fetches provider model catalogs, reconciles them with local model metadata, and exposes synchronized availability back to the rest of the app. The architecture explicitly mentions `OpenAiModelCatalogService` as part of the recommended third migration batch. This work should sit on top of the local model repository from Step 08 and the transport infrastructure from Steps 10 and 11. It should not duplicate local selection logic or UI-specific formatting. The outcome should be a focused service and use-case layer for remote catalog refresh, normalization, and merge behavior.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 03, 06, 08, 10, and 11
- Any remote transport or repository interfaces already created for models
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the legacy equivalents of:

- `OpenAiModelCatalogService`
- Any provider model-list fetchers
- Any sync or merge helper that compares remote catalog entries with local installation state or user selections

Read every branch. Remote catalog logic often hides subtle behavior around filtering, display order, capability flags, and stale selections.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the old code fetches model catalog data and from which providers.
2. Which remote fields matter for the app and which are ignored.
3. How local and remote model metadata are merged.
4. How the old code handles missing credentials, unsupported providers, empty catalogs, or duplicate model IDs.
5. Which branches update persistent local state and which are just transient fetch results.

List every business rule that can change what models the user sees or can select.

### Step 2: Translation

Implement the service layer under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/modelcatalog/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/` only if a repository extension is required for persisting sync outcomes

Expected outcomes:

- A remote catalog service for the relevant provider or providers.
- Mapper logic that normalizes remote catalog entries into the domain model language created earlier.
- A synchronization use case that merges remote entries with local installation state and updates persistence if the legacy behavior did so.
- Clear failure and empty-state handling that other layers can react to.

Do not start UI migration in this step. Keep the outputs domain-focused and reusable.

### Step 3: Self-Check

Compare the new service with the old one and state:

- Whether catalog filtering, sorting, and merge rules remained equivalent.
- Whether any remote field or provider branch was intentionally left out.
- Whether stale local selection behavior still works when the remote catalog changes.
- Whether error cases such as no token, no connectivity, duplicate IDs, or empty lists are still handled explicitly.

## Implementation Instructions

1. Reuse the local model repository rather than rebuilding local state logic in this service.
2. Keep remote normalization separate from persistence updates so the merge behavior is easy to audit.
3. Use result and error contracts consistently. Avoid surfacing raw `Ktor` exceptions directly to features.
4. Preserve deterministic ordering if the old app had a stable model list.
5. If provider-specific catalog shapes differ, normalize them into one clear domain representation instead of leaking raw DTOs out.
6. Do not let UI labels or styling concerns creep into this step.
7. Register the service and sync use case cleanly in DI.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/modelcatalog/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- Remote model catalog fetching exists.
- Remote entries are normalized into domain models.
- Local and remote model state can be synchronized in a controlled way.
- Error and empty states are explicit.
- The UI can later consume catalog data without knowing HTTP details.

## Handoff Rule

Mark only Step 12 complete. Do not start monitoring state or screen migration in the same turn. This service layer should remain independently reviewable.
