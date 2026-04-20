# Step 18: Translate the Monitoring Feature UI and Integrate It into the Shared Root

## Objective

This step is the final major shared-UI migration. It binds the monitoring session logic from Steps 13 through 15 to a real Compose Multiplatform feature screen. The old Android monitoring or main screen probably contains the densest concentration of state, diagnostics, action buttons, live transcript output, and answer generation feedback. That is exactly why it must be translated only after the shared monitoring engine, repositories, and root shell already exist. The outcome of this step should be a cross-platform monitoring feature that reads from shared `UiState`, emits user intents, and fits naturally into the Decompose root navigation.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 04, 13, 14, 15, and 16
- Any shared monitoring feature contracts under `shared`
- `composeApp` navigation and presenter patterns already created
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the old Android main or monitoring screen, related compose components, and any view-model or controller glue. Read line by line, especially around start or stop controls, live transcript sections, answer cards, status banners, and permission or capability warnings.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which monitoring states the screen must render.
2. Which user actions exist, such as start, stop, pause, retry, clear history, or inspect diagnostics.
3. How the old UI represents live transcript updates, pending answers, final answers, and errors.
4. Which warnings depend on permissions, unsupported capabilities, or missing configuration.
5. Which branches are presentation details and which encode feature-level business behavior.

List every rule that affects button availability, banners, progress indicators, transcript rendering, and answer display.

### Step 2: Translation

Implement the feature slice under paths such as:

- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/monitoring/`
- `composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/navigation/`

Expected outcomes:

- A monitoring presenter or component that consumes the shared session manager outputs and exposes a screen-ready `UiState`.
- A screen that renders monitoring status, transcript, answer output, and error or unsupported states from pure state.
- Intents for start, stop, retry, settings navigation, and any other user actions present in the old UI.
- Full integration into the shared root shell so this can serve as the main functional destination.

Do not implement Android runtime permission launchers directly in the screen. If the UI needs to request platform actions, emit an intent or effect that a later bridge can handle.

### Step 3: Self-Check

Compare the new feature with the original monitoring UI and state:

- Whether every major monitoring state and action is represented.
- Whether live transcript and answer rendering preserved the old behavior.
- Whether permission, capability, and missing-config warnings are still visible.
- Whether any Android-specific interactions were deferred to the later platform bridge step and documented clearly.

## Implementation Instructions

1. Keep the screen reactive and declarative. The presenter owns branching and derived labels.
2. Avoid direct repository or service calls from composables.
3. Reuse root navigation, design-system components, and presenter patterns established in prior UI steps.
4. Preserve enough diagnostic detail in the `UiState` for users to understand why monitoring cannot run.
5. Keep all platform-triggering actions as intents or effects rather than direct calls to Android APIs.
6. If the old screen had sections that can be split into child composables, do that only when it improves clarity; do not over-fragment.
7. Ensure the root shell can treat this feature as the main destination without requiring Android-only plumbing.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/presenters/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/screens/monitoring/`
- `KMP_Project/composeApp/src/commonMain/kotlin/com/classroomassistant/composeapp/navigation/`

You may touch `shared` only if a tiny missing feature contract prevents clean UI binding.

## Completion Checklist

- The monitoring feature has a shared presenter and shared screen.
- Live monitoring state, transcripts, and answers are represented.
- Unsupported, permission-blocked, and error states are visible.
- The feature is integrated into root navigation.
- No Android-only APIs leak into the Compose screen.

## Handoff Rule

Mark only Step 18 complete. Do not start platform actual implementations or final hardening in the same turn. The shared monitoring UI deserves a standalone review checkpoint.
