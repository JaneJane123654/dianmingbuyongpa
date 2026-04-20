# Step 11: Translate the App-Facing LLM Client and Provider Routing Logic

## Objective

This step takes the raw transport layer from Step 10 and turns it into the app-facing `LLMClient` behavior that features will actually call. The architecture and migration playbook both highlight `LLMClient`, `DefaultLLMClient`, and provider-routing logic as a dedicated migration batch. This step is where the AI worker must preserve how the old app selects a provider, chooses the active model, resolves credentials, prepares prompts, performs a request, and converts transport responses into domain-level answer results. It is intentionally later than DTO translation so the transport contract is already fixed and easier to reason about.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 03, 06, 07, 08, and 10
- Any prompt-template entities or config repositories already created
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the legacy equivalents of:

- `LLMClient`
- `DefaultLLMClient`
- Any provider router, request builder, or answer-generation service
- Any helper that merges `PromptTemplate`, user config, and selected model into one request

Read line by line. The important work here is behavior preservation, not merely signature translation.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the old code decides which provider and model to use.
2. Which config fields are required before a request may be sent.
3. How the old code builds final prompts, message lists, or request options from templates and runtime input.
4. How errors are classified when credentials are missing, the provider is unknown, the network fails, or the response is empty.
5. Whether the old client supports synchronous answers, streamed answers, retries, or provider fallback.

List every branch that changes business behavior. Do not describe only the nominal request path.

### Step 2: Translation

Implement the app-facing client under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/` only if a narrow repository adapter is required

Expected outcomes:

- An `LLMClient` interface or equivalent app-facing contract.
- A `DefaultLlmClient` or similarly named implementation that composes settings, selected model metadata, prompt templates, credentials, and the Step 10 API layer.
- Explicit provider-routing logic that remains transparent and testable.
- Domain-level answer or completion mapping that other features can consume without knowing transport details.

Do not yet implement remote model catalog synchronization or session orchestration in this step.

### Step 3: Self-Check

Compare the new client with the legacy behavior and state:

- Whether every provider-selection branch still exists.
- Whether missing-config and missing-credential failures are still surfaced correctly.
- Whether prompt assembly preserved all business conditions and placeholders.
- Whether any retry, streaming, or fallback behavior was simplified or intentionally deferred.

## Implementation Instructions

1. Keep provider routing explicit. Avoid burying it in long `when` chains mixed with request serialization if a dedicated mapper or strategy object is clearer.
2. Use repository outputs from earlier steps rather than re-reading raw settings or raw SQL directly.
3. Reuse the Step 01 result and error contracts so feature code gets a consistent failure model.
4. Keep the client in `shared/commonMain`; the only platform-specific pieces should already be abstracted by repositories and platform gateways.
5. If the old code supported provider capability flags, preserve them in a form that later UI can query.
6. Do not add unrelated answer-history persistence here unless the old client already owned that behavior and it cannot be separated cleanly.
7. Update DI registrations so the client is injectable, but do not wire full feature components yet.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/usecase/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- The app has a typed, injectable LLM client.
- Provider routing is explicit and testable.
- Prompt-template and config inputs are integrated.
- Errors for missing config, missing credentials, provider mismatch, and empty response are mapped clearly.
- Step 12 can now focus purely on model catalog syncing instead of request execution.

## Handoff Rule

Mark only Step 11 complete. Do not start model catalog syncing or session-manager translation in the same turn. This client layer needs a clean review boundary.
