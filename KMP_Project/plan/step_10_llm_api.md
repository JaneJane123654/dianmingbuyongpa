# Step 10: Translate LLM DTOs and the OpenAI-Compatible API Layer

## Objective

This step begins the remote AI integration proper, but it is still deliberately narrower than a full client implementation. The goal is to express provider request and response payloads, endpoint definitions, and transport-level mapping as a clean `Ktor` API layer. The architecture explicitly recommends placing provider DTOs under `shared/data/remote/llm/dto` and keeping transport code separate from domain use cases. This step should therefore translate old request or response classes, serializer assumptions, and endpoint shapes into KMP-safe DTOs and thin API wrappers, without yet deciding final business routing or prompt orchestration behavior.

## Required Reading Before Writing Code

Read these files first:

- Outputs from Steps 01, 03, 05, and 06
- Any shared network infrastructure created in Step 03
- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`

Then inspect the legacy equivalents of:

- `LLMConfig`
- `LLMClient` request and response models
- Any OpenAI-compatible request payload classes
- Any raw HTTP helper classes for chat completions, embeddings, model lists, or response parsing

Read them line by line. Your job is to extract transport contracts, not to collapse business rules into DTOs.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which endpoints the old code calls and what each one is used for.
2. How the old code structures prompts, messages, options, and tool arguments in transport payloads.
3. Which response fields are required for downstream business logic and which are optional metadata.
4. How parse failures, missing fields, empty choices, or provider-specific variants are handled.
5. Whether multiple providers share one compatible payload contract or require branching DTOs.

List every field and every error or fallback branch that affects later translation.

### Step 2: Translation

Implement transport-layer code under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/dto/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/mapper/`

Expected outcomes:

- `@Serializable` DTOs for the old request and response payloads.
- A thin API wrapper or endpoint interface for OpenAI-compatible calls built on the shared `HttpClientFactory`.
- Mapping helpers that convert between transport DTOs and intermediate domain-friendly shapes where needed.
- Explicit handling for nullable fields, empty lists, and provider quirks that were present in the legacy code.

Do not yet implement the final `LLMClient` abstraction that the rest of the app will consume. That belongs to Step 11.

### Step 3: Self-Check

Compare the new transport layer with the old one and state:

- Whether every request field has a matching serialized representation.
- Whether any provider-specific field was deferred and why.
- Whether response parsing still handles empty or partial payloads correctly.
- Whether all transport-level failure branches have a place to surface into the Step 01 result and error model.

## Implementation Instructions

1. Use `kotlinx.serialization` consistently. Do not mix in Gson or manual JSON parsing.
2. Keep DTOs separate from domain models. This step is specifically about the remote contract boundary.
3. Preserve field names and nullability carefully. If the old API depends on optional values or missing properties, model that explicitly.
4. Prefer small, dedicated mapper functions over giant all-in-one translation methods.
5. Use the network infrastructure from Step 03; do not instantiate ad hoc `HttpClient`s.
6. Avoid premature provider routing. If there are multiple compatible providers, represent them as configuration to the API layer rather than business decisions here.
7. Add concise comments only where a transport quirk would otherwise be easy to misread.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/` if a transport-layer registration is needed

## Completion Checklist

- Remote LLM DTOs are defined with `@Serializable`.
- A reusable OpenAI-compatible API layer exists.
- Transport mapping is explicit and separate from domain logic.
- No final provider routing or app-facing client orchestration was implemented early.
- Step 11 can now build a real app-facing `LLMClient` on top of this layer.

## Handoff Rule

Mark only Step 10 complete when done. Do not jump ahead to user-facing LLM orchestration in the same turn. The DTO and API boundary must stay isolated and reviewable.
