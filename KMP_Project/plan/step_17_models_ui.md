# Step 17: Translate the Models Feature into Shared Presenters, State, and Compose Screen

## Objective

This step translates the model-management user experience into the shared Compose layer. By this point, the project should already have local model state, remote catalog sync, and model-selection repositories. The remaining work is to give the user a cross-platform models screen that displays available models, installation state, active selection, and sync or refresh actions without leaking transport or storage logic into Compose. The old Android UI may already be largely Compose-based, which means this translation should preserve structure where practical while still enforcing the KMP rules from the migration playbook.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 04, 08, 12, and 16
- Any shared model feature contracts under `shared`
- The root navigation setup from Step 04
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the old Android model-management screen, any related compose components, list item renderers, view models, and dialogs. Read line by line and note every branch tied to installation state, selected state, remote sync, or unsupported capability.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which model states the UI needs to show.
2. How the old screen distinguishes local models, remote models, selected models, and unavailable models.
3. Which actions exist, such as refresh, select, install, remove, or inspect.
4. How the old screen responds to sync failure, empty catalog, missing credentials, or unsupported features.
5. Which parts of the old UI are presentation-only versus those that encode business decisions.

List every condition that changes item rendering, action availability, or warning text.

### Step 2: Translation

Implement the feature slice under paths such as:

- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/models/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/models/` only if a shared feature adapter is needed

Expected outcomes:

- A models presenter or component that uses the model repository and catalog sync use case.
- A stable `UiState` describing the list, the selected model, loading state, and actionable errors.
- A pure Compose screen that renders the state and emits intents.
- Clean root navigation integration so the feature can be opened from the shared shell.

Do not implement installation binaries, downloader engines, or platform-specific file pickers here unless the old screen only needs a bridge callback placeholder.

### Step 3: Self-Check

Compare the new feature with the original models screen and state:

- Whether every model state and item-action branch is represented.
- Whether sync failure, empty state, and unsupported-feature handling remained explicit.
- Whether any Android-only interaction was deferred to a later platform bridge.
- Whether the new `UiState` captures enough detail to render the original UX faithfully.

## Implementation Instructions

1. Keep list rendering dumb and declarative. All branching should be derived from the presenter `UiState`.
2. Prefer explicit item models over reaching into domain entities directly from the screen when formatting or grouping is involved.
3. Use the shared model repository and sync use case, not raw transport code.
4. Preserve stable item IDs and ordering if the old UI depends on them.
5. Keep the screen platform-neutral and free of `Context`, `Intent`, or filesystem APIs.
6. Reuse design-system components or patterns already established in the project.
7. Register the feature cleanly in the root navigation so later smoke tests can reach it.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/models/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/navigation/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/models/` if required

## Completion Checklist

- The models feature has a shared presenter and shared screen.
- Model list state, selection, and refresh actions are represented cleanly.
- Error, empty, and loading states are explicit.
- No transport or platform APIs leak into the Compose screen.
- Root navigation can open the models feature.

## Handoff Rule

Mark only Step 17 complete. Do not start the monitoring screen in the same turn. Monitoring is the riskiest UI and deserves its own isolated pass.
