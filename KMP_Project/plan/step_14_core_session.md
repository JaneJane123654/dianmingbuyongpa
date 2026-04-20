# Step 14: Translate CoreSessionManager into Coroutine-Based Shared Logic

## Objective

This step translates the lower-level session orchestration logic from the legacy app into `shared/commonMain`. The migration playbook explicitly recommends moving `CoreSessionManager` after the state models are in place and converting listeners to `StateFlow` or `SharedFlow`, threads to coroutines, and mutable Java coordination into explicit immutable state updates. This step should focus on the inner orchestration engine: the part that reacts to recognition inputs, coordinates answer generation requests, emits monitoring events, and updates session state. It should not yet be responsible for classroom- or feature-level policy that belongs to the higher `ClassSessionManager`.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 01, 05, 09, 11, and 13
- Any recording and answer-history repositories already created
- Any monitoring use-case contracts already defined

Then inspect the legacy equivalents of:

- `CoreSessionManager`
- Any inner monitoring pipeline coordinator
- Any helper classes it directly owns for recognition callbacks, answer request triggering, or event fan-out

Read line by line. Treat this as a state-machine translation, not just a syntax conversion.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. What state the old `CoreSessionManager` owns directly.
2. Which inputs it consumes and which outputs it emits.
3. How it sequences recognition results, answer requests, repository writes, and state updates.
4. How it handles cancellation, restart, duplicate triggers, overlapping answers, and error recovery.
5. Which conditions are pure mechanics and which are actual business rules that must be preserved exactly.

List every branch and every transition guard. This step lives or dies by state-machine fidelity.

### Step 2: Translation

Implement the manager and its close collaborators under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/service/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/` only if a small orchestration helper contract is necessary

Expected outcomes:

- A coroutine-based `CoreSessionManager` or equivalent orchestrator.
- `StateFlow` for durable session state and `SharedFlow` for transient events.
- Explicit cancellation handling for long-running jobs.
- Clear interaction with repositories and the app-facing `LLMClient`.

Do not yet translate classroom-level feature policies, screen intent handling, or platform permission prompting. Keep this step centered on inner orchestration.

### Step 3: Self-Check

Compare the new manager with the original one and state:

- Whether every state transition and guard condition still exists.
- Whether listener and callback behavior was fully represented using flows.
- Whether cancellation and restart semantics changed.
- Whether any edge case such as duplicate transcript bursts, empty answers, or repository write failures was simplified.

## Implementation Instructions

1. Use structured concurrency. Avoid untracked background jobs.
2. Keep mutable state private and expose immutable flows.
3. Preserve ordering guarantees if the old code relied on them, especially around transcript events and answer generation.
4. Use the repositories and clients created earlier instead of re-implementing storage or network concerns.
5. If the legacy manager mixed platform callbacks directly into business logic, peel that apart now and keep only shared orchestration here.
6. Emit domain events that are diagnostic-friendly. Later UI and logs should not need to reverse-engineer internal state changes.
7. Register the manager and its dependencies in DI only when the shape is stable.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/feature/monitoring/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/service/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- The inner monitoring session engine exists in `shared/commonMain`.
- State and event output use flows instead of callback lists.
- Cancellation and restart are explicit.
- Repository and LLM client interactions are clear and bounded.
- Step 15 can now add classroom-level orchestration on top of this shared engine.

## Handoff Rule

Mark only Step 14 complete. Do not start feature-level session policy or screen wiring in the same turn. This manager needs to be evaluated in isolation first.
