# Step 16: Translate the Settings Feature into Shared Presenters, State, and Compose Screen

## Objective

This step moves the settings feature into the shared Compose layer. The architecture says that `composeApp` should own shared UI, navigation, theme, and screen state mapping, while `shared` owns the business repositories and feature logic. By the time you reach this step, the settings repositories, entities, and root shell should already exist. The task now is to translate the old Android settings screen into a KMP-safe feature slice composed of a presenter or component, a `UiState`, and a platform-neutral Compose screen. This step must not smuggle Android-specific APIs such as `LocalContext`, `Toast`, or `ActivityResult` into `commonMain`.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 04, 06, and 07
- Any feature-level settings contracts already exposed from `shared`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/app/` and `navigation/`
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the old Android settings screen and any related view models, presenters, or controllers. Read the screen code line by line, including each settings section, toggle, text field, validation path, and save behavior.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which settings are visible to the user and why.
2. Which controls are simple read/write toggles versus those with validation or side effects.
3. Which parts of the old screen are pure UI and which are really business logic.
4. How the old screen reacts to invalid input, missing credentials, unsupported features, or permission dependencies.
5. Which screen states need to survive configuration or navigation changes.

List every business rule behind button enablement, warning text, save actions, reset actions, and error display.

### Step 2: Translation

Implement the feature slice under paths such as:

- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/settings/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/settings/` only if a small shared feature contract is missing

Expected outcomes:

- A settings presenter or component that talks to the typed repositories from Step 07.
- A stable `UiState` and intent model.
- A pure Compose screen that renders only from `UiState` and raises intents or callbacks.
- Proper integration with the root navigation from Step 04.

Do not implement platform permission launchers or file pickers directly in `commonMain`. If the old screen triggered native actions, route them through existing abstractions or leave a clearly named bridge point.

### Step 3: Self-Check

Compare the new feature with the original settings screen and state:

- Whether every visible setting and validation branch is represented.
- Whether any Android-only UX behavior was deferred to a platform bridge.
- Whether `UiState` still carries enough detail to render warnings, errors, and save status.
- Whether any business rule was simplified and why.

## Implementation Instructions

1. Follow the recommended three-layer UI pattern from the migration playbook: `Component`, `UiState`, `Screen`.
2. Keep the presenter free of Android APIs and keep the screen free of business logic.
3. Reuse the design system and root shell created earlier rather than rebuilding app scaffolding.
4. Preserve Material 3 structure where it maps cleanly to Compose Multiplatform.
5. Translate listeners and callbacks into state updates and intent handling.
6. Avoid accidental coupling to unfinished features such as model catalog sync unless the old settings screen truly depends on them.
7. Register any presenter or component dependencies cleanly in DI or the root component factory.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/settings/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/navigation/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/settings/` if strictly required

## Completion Checklist

- The settings feature renders from shared `UiState`.
- The presenter talks to typed repositories, not raw storage.
- No Android-only APIs leak into `commonMain`.
- Root navigation can open the settings feature.
- The user-visible logic matches the old screen.

## Handoff Rule

Mark only Step 16 complete. Do not start models or monitoring UI in the same turn. Each feature screen needs a separate migration checkpoint.
