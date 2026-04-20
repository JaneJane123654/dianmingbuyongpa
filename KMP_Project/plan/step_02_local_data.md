# Step 02: Build the Local Data Foundation for Settings, History, and Model State

## Objective

This step establishes the local persistence layer that later repositories will use. The current scaffold already ships with `SQLDelight`, `Multiplatform Settings`, and a starter `AppDatabase.sq` that contains `setting_entry` and `monitoring_event`. That is useful, but not enough. According to the architecture document, the KMP project must eventually persist structured settings, monitoring events, recording metadata, model installation state, and answer history. This step turns the starter persistence scaffold into a stable local data platform without yet translating high-level repository business logic. Think of this as preparing the storage rails before trains start running on them.

## Required Reading Before Writing Code

Read these KMP files first:

- `docs/ARCHITECTURE.md`
- `docs/MIGRATION_PLAYBOOK.md`
- `shared/build.gradle.kts`
- `shared/src/commonMain/sqldelight/com/classroomassistant/shared/db/AppDatabase.sq`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/storage/PlatformDirectories.kt`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/secure/SecureStore.kt`

Then inspect the original legacy storage-oriented classes in the old project. The highest-value reference targets are the equivalents of `ConfigManager`, `PreferencesManager`, recording metadata persistence, downloaded model bookkeeping, and any answer or session history store. Read them line by line and treat them as translation input only. Do not modify them.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Before writing code, explain in English:

1. Which kinds of data the old app stores locally and why.
2. Which values are high-frequency key-value settings versus structured relational history.
3. How the legacy app handles absent values, migrations, or corrupted cache entries.
4. Whether recording metadata and model installation state are append-only, overwrite-in-place, or derived from filesystem scans.
5. Any business rule that depends on timestamps, ordering, uniqueness, or cleanup behavior.

Your analysis must explicitly separate what should live in `Multiplatform Settings` from what should live in `SQLDelight`.

### Step 2: Translation

Create the local persistence foundation under packages such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/db/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/settings/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/model/`
- `shared/src/commonMain/sqldelight/com/classroomassistant/shared/db/`

This step should usually include:

- Expanding `AppDatabase.sq` so it matches the architecture recommendation for `recording_entry`, `model_installation`, and `answer_history`, while keeping `setting_entry` and `monitoring_event`.
- Adding any database helper abstractions that KMP needs, such as a `DatabaseDriverFactory` expect/actual boundary if the generated driver cannot be wired cleanly otherwise.
- Introducing local entity or record models used specifically by the persistence layer, not domain models yet.
- Preparing typed settings keys or settings namespaces so later repositories do not hardcode raw strings everywhere.

Do not yet implement the public repositories consumed by features. This step is about local data plumbing only.

### Step 3: Self-Check

Compare the new persistence foundation with the old storage logic and list:

- Whether any legacy table or file-backed concept was intentionally deferred.
- Whether any file-based data in the old app is now modeled as structured database rows.
- Whether timestamp units, key names, or retention behavior changed.
- Whether all old business branches for “missing”, “default”, “deleted”, and “corrupted” local state still have an equivalent path forward.

## Implementation Instructions

1. Use `SQLDelight 2.3.2` and `Multiplatform Settings 1.3.0`, because those versions are already fixed in `gradle/libs.versions.toml`.
2. Keep the database schema explicit and readable. The next AI should be able to infer later repository methods from the SQL file without guessing.
3. Design key-value access around typed models or key descriptors, not free-form string calls scattered through features.
4. Avoid pushing domain mapping into this step. Persistence models can be plain local records for now.
5. If a legacy concept depends on filesystem paths, route the path resolution through `PlatformDirectories` instead of hardcoding Android or iOS file APIs.
6. Preserve deterministic IDs and timestamp fields wherever the old logic depends on stable ordering or deduplication.
7. Add only the minimum DI hooks needed so future repository steps can obtain database and settings instances cleanly.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/`
- `KMP_Project/shared/src/commonMain/sqldelight/com/classroomassistant/shared/db/`
- `KMP_Project/shared/src/androidMain/kotlin/com/classroomassistant/shared/data/local/` if an actual driver helper is required
- `KMP_Project/shared/src/iosMain/kotlin/com/classroomassistant/shared/data/local/` if an actual driver helper is required
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/` for minimal local data registrations

## Completion Checklist

- The database schema covers settings, monitoring events, recording entries, model installations, and answer history.
- Key-value settings access has a typed foundation.
- No repository business rules are implemented prematurely.
- No platform-specific storage APIs leak into `commonMain`.
- The resulting local data layer is ready for Steps 07, 08, and 09 to build repositories on top of it.

## Handoff Rule

When this step is done, update only the corresponding checkbox in the memory document. Do not start translating `ConfigManager` or `ModelRepository` yet. Those repositories should be implemented only after the storage rails are stable.
