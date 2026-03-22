# ZEconomy Roadmap

This roadmap tracks the technical direction for stabilizing, optimizing, and maturing the mod as a long-lived project.

## Phase 1: Command and Service Cleanup

Target: `1.2.x`

- Break the command layer into smaller thematic units.
- Keep command handlers focused on parsing, permission checks, and player/admin feedback.
- Continue moving domain logic into shared services.
- Standardize when operations trigger refresh, sync, and save behavior.
- Remove direct low-level state traversal from UI, network, and command layers where possible.

## Phase 2: Performance and Hot Path Optimization

Target: `1.3.x`

- Eliminate repeated full rescans of reserved bank balances.
- Audit all high-frequency economy paths for redundant sync, serialization, and data traversal.
- Tighten autosave behavior and verify snapshot consistency.
- Reduce heavy work on the main server thread.
- Revisit storage backends, especially SQL modes, for expensive full-state writes.

## Phase 3: Transactional Economy Core

Target: `1.4.x`

- Introduce a unified transaction-style flow for economy operations.
- Separate validation, state mutation, sync, and persistence concerns.
- Use explicit structured results for core operations.
- Reduce hidden side effects in low-level helper methods.

## Phase 4: Data Integrity and Recovery

Target: `1.4.x` to `1.5.x`

- Define invariants for wallet, bank, vault, treasury, and mirror data.
- Strengthen repair and reconciliation tools.
- Add migration-aware handling for stored data.
- Verify recovery behavior for interrupted saves and damaged storage files.

## Phase 5: Automated Verification

Target: `1.5.x`

- Add tests for transfer, bank, vault, exchange, daily reward, and treasury reservation behavior.
- Add regression coverage for repaired bugs.
- Add serialization and storage roundtrip checks.

## Phase 6: Storage Architecture

Target: `1.5.x`

- Separate runtime state from persistence representations.
- Strengthen the storage backend abstraction.
- Improve SQL handling and reduce unnecessary full rewrites where possible.
- Expand storage diagnostics and observability.

## Phase 7: API Maturity

Target: `1.6.x`

- Review and stabilize the public API contract.
- Standardize API errors and operation results.
- Expand API documentation and integration examples.
- Clearly mark stable versus internal behavior.

## Phase 8: Admin UX and Gameplay Polish

Target: `1.6.x` to `1.7.x`

- Improve admin command output and diagnostics.
- Refine GUI interactions for bank, vault, mailbox, and exchange flows.
- Expand configurability for sync, save, and interest policies.
- Improve logging and operator visibility for important runtime events.

## Phase 9: Release and Maintenance Discipline

Target: `1.7.x+`

- Keep changelog and migration notes consistent across releases.
- Expand setup, config, and command documentation.
- Maintain repeatable release validation checklists.
- Track known issues and compatibility notes for server operators.

## Current Focus

Active work is currently focused on Phase 1, starting with command-layer decomposition and continued service extraction.
