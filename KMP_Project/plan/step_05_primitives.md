# Step 05: Migrate Default Values, Error Codes, and Session Primitives

## Objective

This step begins the actual domain-model translation by moving the most stable, low-risk primitives first. The migration playbook explicitly recommends starting with constants, core model types, prompt scaffolding, and configuration data before translating services. This step should harvest the old project’s default configuration classes and session-related enums or simple state models, then express them as clean Kotlin Multiplatform types. These models will anchor later repositories, session managers, and presenters. Done well, this step sharply reduces ambiguity for every remaining translation task.

## Required Reading Before Writing Code

Read these KMP files first:

- The core result and error contracts from Step 01
- `docs/ARCHITECTURE.md`, especially sections on migration batch order and package mapping
- `docs/MIGRATION_PLAYBOOK.md`, especially the first and fourth migration rounds
- Any current permission and capability primitives under `shared/platform`

Then inspect the original legacy classes or enums equivalent to:

- `ErrorCode`
- `SessionState`
- `AudioConfig`
- `VadDefaults`
- `AiDefaults`

If the old code names differ, use the actual legacy equivalents, but keep the new KMP naming consistent with the architecture and current package conventions.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Read each legacy primitive line by line and explain in English:

1. Which values are pure defaults and which values are business-critical thresholds.
2. Which error codes drive user-visible behavior, retry logic, or capability gating.
3. Whether `SessionState` is a flat enum, a richer state object, or a partially implicit state machine.
4. Which audio or VAD constants are tuned for latency, quality, energy use, or silence detection.
5. Any edge case where the old app treats “idle”, “starting”, “listening”, “processing”, “answering”, or “failed” differently.

You must explicitly list all business decision points, not just the happy path names.

### Step 2: Translation

Translate these primitives into `shared/commonMain` packages such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/core/error/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/core/model/`

Expected outputs:

- An `ErrorCode` style enum or sealed type compatible with the Step 01 error envelope.
- A KMP-safe representation of session lifecycle state.
- Audio and AI default configuration models expressed as immutable Kotlin data.
- Clear naming for thresholds and defaults so later service code can consume them without duplicating magic numbers.

Do not yet translate repositories or stateful managers. This step must stop at low-level immutable models and default sets.

### Step 3: Self-Check

Compare the translated primitives with the original ones and state:

- Whether any constant was renamed, merged, or split.
- Whether any “unknown”, “unsupported”, or “error” state from the old code was dropped.
- Whether numeric units changed, for example milliseconds versus seconds or bytes versus frames.
- Whether every legacy branch that depended on an error code or session state can still be expressed exactly.

## Implementation Instructions

1. Keep these models immutable and side-effect free.
2. Prefer strongly typed configuration groups over isolated top-level constants when the values conceptually belong together.
3. Preserve legacy thresholds exactly unless there is a documented reason to normalize units. If you normalize, document it clearly in code comments and self-check output.
4. Place domain-facing models under `domain/model` and cross-cutting primitives under `core/*` according to the architecture.
5. Avoid leaking Android-specific audio types, Java `Properties`, or platform enums into these models.
6. Prepare for later session-manager steps by making session-state representation expressive enough for monitoring orchestration, but do not encode mutable transitions here.
7. If the old app had ambiguous or duplicated constant classes, choose the most authoritative source and note the consolidation.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/core/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`

Minor import adjustments elsewhere are allowed only if needed to compile new model references.

## Completion Checklist

- Error codes are represented in KMP.
- Session primitives are represented in KMP.
- Audio, VAD, and AI defaults are represented in KMP.
- All outputs are immutable and platform-free.
- Later steps can consume these types without guessing legacy semantics.

## Handoff Rule

When complete, mark only Step 05 as done. Do not start repository or service logic in the same pass. This step exists to freeze the stable primitive vocabulary before behavior translation begins.
