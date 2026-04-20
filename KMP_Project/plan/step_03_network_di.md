# Step 03: Establish Network, Serialization, and DI Foundation

## Objective

This step prepares the shared networking and dependency injection infrastructure that all AI-provider and model-catalog work will later depend on. The architecture has already locked the project to `Ktor`, `kotlinx.serialization`, and `Koin`, and the Gradle files confirm those dependencies are present. What is missing is the shared foundation that decides how JSON is configured, how `HttpClient` instances are created, how authentication hooks will later plug in, and how the project separates `platformModule`, `dataModule`, and `featureModule`. If this is not standardized now, later service translation steps will each invent a slightly different `HttpClient`, error-mapping strategy, and DI registration pattern.

## Required Reading Before Writing Code

Read these files first:

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/di/SharedModules.kt`
- Any core contracts created in Step 01
- Any local data primitives created in Step 02

Then inspect the original legacy network helpers, API clients, provider routing utilities, JSON adapters, and any DI or service locator equivalents. Read them line by line. You are not allowed to migrate provider-specific business endpoints yet, but you must understand how the old code configures headers, retries, base URLs, model routing, and authentication.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. How the old app creates and reuses HTTP clients.
2. Which serialization library and field naming assumptions the old APIs depend on.
3. How authorization tokens or API keys are attached.
4. Whether there are special timeout, retry, or logging behaviors for model APIs.
5. Which dependencies are true cross-feature infrastructure versus provider-specific logic that should be deferred.

Be explicit about every branch where the legacy code handles empty base URLs, missing tokens, rate-limit responses, parse failures, or unknown provider names.

### Step 2: Translation

Create the infrastructure under packages such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/core/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

Typical outputs for this step should include:

- A single shared `Json` configuration tuned for stable KMP serialization.
- A reusable `HttpClientFactory` or similarly named builder that installs content negotiation, logging, and auth scaffolding in one place.
- Clear dependency injection modules that separate platform bindings from data-layer and feature-layer bindings.
- If useful, a small auth provider or request decoration contract that later LLM services can implement without hardcoding token lookup into every client.

Do not implement `LLMClient`, OpenAI endpoints, DTOs, or catalog syncing yet. This step must stop at infrastructure.

### Step 3: Self-Check

Compare the new infrastructure with the old helpers and state:

- Whether timeout, logging, and auth behavior remained equivalent.
- Whether you deferred any provider-specific conditionals that existed in the old code.
- Whether any legacy retry logic was intentionally postponed to a later, more domain-aware service step.
- Whether all known failure paths still have a place to be modeled using the Step 01 core result and error contracts.

## Implementation Instructions

1. Use the versions already fixed in the repository: `Ktor 3.4.1`, `kotlinx.serialization 1.9.0`, and `Koin 4.1.1`.
2. Prefer a single source of truth for client creation. Do not allow future services to instantiate raw `HttpClient()` ad hoc.
3. Keep `SharedModules.kt` readable. It is fine to refactor it into `platformModule`, `dataModule`, and `featureModule` helpers if that improves clarity and matches the architecture.
4. Preserve room for test doubles. Even if you do not write tests in this step, the factory and DI design should make later unit tests straightforward.
5. Keep secrets out of client constructors where possible. Authentication values should come from repositories or injected providers rather than string literals.
6. Network infrastructure belongs in `shared/commonMain`. Do not use Android-only logging, interceptors, or iOS-only types in this layer.
7. If a legacy network helper bundled business rules with raw HTTP code, split them now and keep only the truly reusable transport concerns.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/remote/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`
- Supporting imports in `shared/build.gradle.kts` only if the current dependency list is insufficient for the infrastructure you are implementing

Do not touch `composeApp` in this step.

## Completion Checklist

- There is one shared JSON configuration strategy.
- There is one shared `HttpClient` construction strategy.
- DI is organized into stable modules that later steps can extend.
- No provider-specific endpoint code was implemented early.
- The infrastructure is ready for Steps 10, 11, and 12.

## Handoff Rule

Mark only Step 03 complete in the memory document. Do not begin translating DTOs or `LLMClient` in the same turn. The next steps depend on this layer being finished and stable first.
