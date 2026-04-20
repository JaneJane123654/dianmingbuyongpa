# Step 06: Migrate User Preferences, Model Descriptors, Prompt Templates, and LLM Config Entities

## Objective

After the lowest-level primitives are in place, the next safest migration target is the family of immutable entities that describe user-facing configuration, installed or available models, prompt templates, and LLM request configuration. These classes are the bridge between raw storage and real business services. They tend to be stable, they are easy to unit test, and they give future repository and network steps a shared domain language. This step should not yet talk to databases, settings stores, or HTTP clients directly. It should define the entity shapes that those layers will exchange.

## Required Reading Before Writing Code

Read these files first:

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`
- Outputs from Steps 01 through 05
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/capability/PlatformCapabilities.kt`

Then inspect the corresponding legacy classes line by line. Highest-priority sources are the old equivalents of:

- `UserPreferences`
- `ModelDescriptor`
- `PromptTemplate`
- `LLMConfig`
- Any provider enum or model-source enum that decides how a request is routed

If the old code stores some of these as JSON blobs or maps, analyze the exact fields before translating.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which user preferences are functional versus cosmetic.
2. Which model descriptor fields are required for routing, download state, display, and capability matching.
3. How prompt templates are structured and whether placeholders, variables, or role sections exist.
4. Which LLM config values control provider selection, base URL, model name, temperature, timeout, or streaming behavior.
5. Any edge case where the old code falls back to defaults, masks secrets, or disables a provider due to missing capabilities.

List every field and every branch that changes business behavior.

### Step 2: Translation

Implement immutable entity models under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/model/` when a distinct local persistence shape is needed
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/llm/dto/` only if a pure transport DTO is already unavoidable, though the main target of this step is domain entities

Recommended outputs include:

- A clean `UserPreferences` model.
- A `ModelDescriptor` plus any supporting enums such as provider type, source type, installation status, or capability tags.
- A `PromptTemplate` structure that can represent the old prompt logic without stringly typed chaos.
- An `LlmConfig` or equivalent model that later services can consume.

Do not yet implement the mapping from storage or network into these models. This step defines shapes, not translation logic.

### Step 3: Self-Check

Compare with the old entities and explicitly say:

- Whether any field was collapsed, renamed, or split.
- Whether secrets were intentionally excluded from a public model and left for secure storage or repository handling.
- Whether any prompt placeholder or provider flag from the old code is not yet represented.
- Whether all legacy decisions that depend on model metadata or preferences still have a matching field in the new design.

## Implementation Instructions

1. Make the domain models expressive enough that repository code later does not need `Map<String, Any>` fallbacks.
2. Prefer enums or sealed types for provider or category values that are business-significant.
3. Keep prompt-template modeling flexible but disciplined. If the old app supports variable substitution, model it explicitly.
4. Separate secure values from non-secure preferences. Tokens and secrets should not live in a plain public preferences model.
5. Add serialization annotations only where persistence or transport genuinely needs them. Do not over-annotate domain types without reason.
6. Keep platform capability awareness at the metadata level only. Do not call `PlatformCapabilities` from constructors or models.
7. Align package placement with the architecture so the next AI can discover these models easily.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/model/`

Avoid touching service or repository packages in this step.

## Completion Checklist

- User preference entities exist.
- Model descriptor entities exist.
- Prompt-template entities exist.
- LLM configuration entities exist.
- No storage, HTTP, or UI logic leaked into the models.

## Handoff Rule

When done, mark only Step 06 complete. Do not start implementing repository mapping or API calls yet. Later steps rely on these models being finalized first.
