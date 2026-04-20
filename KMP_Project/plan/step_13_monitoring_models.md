# Step 13: Translate Monitoring State Models, Events, and Use-Case Contracts

## Objective

Before moving the legacy session managers themselves, this step extracts the stable monitoring state language that those managers depend on. The architecture and migration playbook both stress that session and monitoring logic should be converted into immutable state plus coroutines or flows, rather than carrying listener-heavy Java orchestration directly into KMP. That means the app needs explicit domain models for monitoring phases, recognition events, answer-generation triggers, user intents, and capability-driven availability before the session managers are translated. This step creates those contracts and only those contracts.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 01, 05, 06, 08, 09, 11, and 12
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/capability/PlatformCapabilities.kt`
- Any recording or answer-history repositories already created

Then inspect the legacy equivalents of:

- Any monitoring event models
- Any recognition pipeline state holders
- Any domain events emitted by `CoreSessionManager`, `ClassSessionManager`, or speech pipeline coordinators
- Any answer-trigger condition models or monitoring use-case interfaces

Read them line by line. Separate immutable state from mutable manager behavior in your notes.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which states the monitoring flow can occupy and how they differ.
2. Which domain events are emitted for audio detection, speech recognition, LLM answer generation, errors, and completion.
3. How the old code determines when a recognition result should trigger an answer.
4. Which events are for UI display only and which drive business decisions.
5. How platform capabilities or missing permissions affect the monitoring state space.

Call out every branch involving idle, warmup, listening, partial transcript, final transcript, answer pending, answer ready, paused, and failed states if they exist.

### Step 2: Translation

Implement the contracts under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`

Expected outcomes:

- Immutable monitoring state models suitable for `StateFlow`.
- Explicit event models suitable for `SharedFlow`.
- Small use-case or command interfaces that define what session managers must provide later.
- Capability-aware availability models or flags if the old code gates monitoring features on platform support.

Do not yet implement the state machine engine or long-running orchestration. This step defines the language the managers will speak.

### Step 3: Self-Check

Compare the new monitoring contracts with the old code and state:

- Whether every state and event that changes business behavior still exists.
- Whether any callback or listener signal was collapsed incorrectly.
- Whether any platform capability branch was lost.
- Whether the new models are rich enough to reproduce the original manager logic without inventing extra ad hoc flags later.

## Implementation Instructions

1. Prefer sealed interfaces and data classes over free-form string events.
2. Distinguish persistent state from transient one-off events clearly.
3. Keep UI naming out of the core domain contracts where possible. These models should work equally well for logging, testing, and UI mapping.
4. Preserve enough detail for later diagnostics, especially around why a monitoring run failed or was paused.
5. If the old code used nested mutable fields, flatten them into explicit immutable value objects when that improves clarity.
6. Do not bind the models to Android microphone APIs or iOS speech APIs.
7. Leave actual orchestration to Steps 14 and 15.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`

## Completion Checklist

- Monitoring states are explicit.
- Monitoring events are explicit.
- Manager-facing contracts exist for later orchestration.
- Capability or permission-driven branches are represented.
- Steps 14 and 15 can now translate manager behavior onto stable immutable models.

## Handoff Rule

Mark only Step 13 complete. Do not start manager implementation in the same turn. These contracts need to be reviewed as the stable basis for the upcoming state machine work.
