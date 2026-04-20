# Step 09: Translate Recording Repository and Monitoring Event Persistence

## Objective

This step migrates the local data logic for recordings and monitoring events. The architecture says that recordings, monitoring events, and answer history belong in structured local storage instead of being hidden behind ad hoc file or preference logic. The goal here is not yet to orchestrate live monitoring; it is to create a repository-level interface for writing and reading recording metadata, event history, and other session-adjacent local traces. Later session-manager steps will rely on these repositories to persist state transitions, diagnostics, and artifacts without worrying about SQL or directories directly.

## Required Reading Before Writing Code

Read these items first:

- Outputs from Steps 02 and 05 through 08
- The SQLDelight schema, especially `monitoring_event`, `recording_entry`, and `answer_history`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/platform/storage/PlatformDirectories.kt`

Then inspect the old project’s equivalents of:

- `RecordingRepository`
- Any recording metadata index, WAV manifest helper, event log helper, or answer history persistence class
- Any cleanup job or retention policy related to recordings and monitoring artifacts

Read line by line and note every place where the old code writes metadata before, during, or after an audio or monitoring session.

## Mandatory Three-Pass Translation Workflow

### Step 1: Analysis

Explain in English:

1. Which pieces of recording data are stored as files versus metadata rows.
2. How monitoring events are categorized and what payloads they carry.
3. Whether answer history is persisted for diagnostics, UX continuity, analytics, or all three.
4. How cleanup, deletion, deduplication, or retention works.
5. Which failures are tolerated, retried, ignored, or surfaced to the user.

Call out every branch involving missing files, partial writes, orphaned metadata, or corrupted event payloads.

### Step 2: Translation

Implement repositories and supporting local mappers under paths such as:

- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/db/`
- `shared/src/commonMain/kotlin/com/classroomassistant/shared/domain/model/` only if a small missing domain type is required

Expected outcomes:

- A repository for recording metadata, including create, update, lookup, and delete or cleanup operations.
- A repository or data source for monitoring events and answer history.
- Explicit mapping between storage payloads and domain-safe event models.
- Clear use of `PlatformDirectories` for file-location resolution while keeping `commonMain` free from raw platform file APIs.

Do not implement live audio capture or session orchestration in this step. Persistence only.

### Step 3: Self-Check

Compare the new repositories with the old behavior and state:

- Whether all create, update, cleanup, and query branches remain covered.
- Whether file and metadata consistency rules stayed intact.
- Whether any payload format or retention behavior changed.
- Whether failure handling is equivalent, especially for partial artifacts and missing files.

## Implementation Instructions

1. Model event kinds explicitly. Avoid leaving event semantics trapped in raw JSON strings if the legacy code depends on structured branching.
2. Keep repositories small and cohesive. Recording metadata, monitoring events, and answer history may share infrastructure, but do not force unrelated logic into one giant class.
3. Preserve timestamps and IDs exactly if they matter for sorting or replay.
4. Translate any listener or callback observation of new recordings or events into coroutine-friendly APIs.
5. If the old app uses lazy file creation, preserve that rather than creating empty files too early.
6. Keep this step strictly local. No remote uploads, no ASR, and no model inference.
7. Register only the persistence collaborators required by this step in DI.

## Allowed Output Scope

You may write only inside:

- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/repository/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/data/local/`
- `KMP_Project/shared/src/commonMain/kotlin/com/classroomassistant/shared/di/`

## Completion Checklist

- Recording metadata is managed through a typed repository.
- Monitoring events and answer history are persisted through clear interfaces.
- File-path resolution remains abstracted.
- No live monitoring orchestration leaked into this step.
- Later session steps can persist events without writing SQL directly.

## Handoff Rule

Mark only Step 09 complete. Do not start translating session managers or UI that consumes this data in the same turn. This repository layer needs to stand on its own first.
