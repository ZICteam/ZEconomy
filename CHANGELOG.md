# Changelog

All notable changes to this project should be documented in this file.

The format is based on keeping entries grouped by released mod version.

## [1.3.4] - 2026-03-22

### Improved

- Added dirty-aware storage tracking in `DataStorageManager` so timer-based autosave skips snapshot work when no persistent economy state changed
- Marked core wallet, bank, vault, rate, mail, and custom-data mutation paths as dirty to keep autosave aligned with actual runtime changes
- Added version-based async save tracking so background writes do not incorrectly treat newer in-flight changes as already persisted

## [1.3.3] - 2026-03-22

### Improved

- Added `CurrencyHelper.scheduleSave(...)` as a shared non-blocking save path for service-level maintenance operations
- Switched reconcile, doctor-fix, and currency repair service flows from immediate synchronous saves to scheduled background saves where strict blocking persistence was not required
- Reduced main-thread save pressure during admin maintenance and repair workflows

## [1.3.2] - 2026-03-22

### Improved

- Reduced hourly bank interest overhead by caching spendable balances per currency during each sweep
- Changed hourly interest payouts to use one bulk currency update per player instead of repeated per-currency bulk updates
- Limited bank mirror syncs during hourly interest processing to players who actually received payouts

## [1.3.1] - 2026-03-22

### Improved

- Centralized bank deposit mutations in `ExtraEconomyData` so cached reserved totals stay synchronized through one code path
- Simplified bank withdrawal validation to use cached spendable balance directly instead of recomputing reserve transitions manually
- Cleaned up zeroed bank deposit entries as part of the cache-backed update path

## [1.3.0] - 2026-03-22

### Changed

- Extracted user economy handlers into [UserEconomyCommands.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/UserEconomyCommands.java)
- Moved wallet, bank, exchange, mail, vault, daily, help, and status user-facing workflows out of `EconomyCommands`
- Completed the main Phase 1 handler split so the central command class now acts much more like a coordinator than a monolith

## [1.2.9] - 2026-03-22

### Changed

- Extracted admin maintenance handlers into [AdminMaintenanceCommands.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/AdminMaintenanceCommands.java)
- Moved admin status, backup, reload storage, export, doctor, inspect, reconcile, and Vault maintenance workflows out of `EconomyCommands`
- Continued Phase 1 handler decomposition by separating operational maintenance commands from the remaining command coordinator

## [1.2.8] - 2026-03-22

### Changed

- Extracted the `admin currency` command handlers and node wiring into [AdminCurrencyCommands.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/AdminCurrencyCommands.java)
- Moved a full functional command group out of `EconomyCommands`, not just registration helpers
- Continued Phase 1 roadmap work by turning command decomposition into a handler-level split

## [1.2.7] - 2026-03-22

### Changed

- Extracted utility command tree wiring into [AdminUtilityCommandTree.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/AdminUtilityCommandTree.java)
- Completed the first command-tree split so user, admin/server, and admin utility registrations now live outside the main command class
- Reduced `EconomyCommands` further toward a coordinator role before the next stage of moving grouped handlers into dedicated classes

## [1.2.6] - 2026-03-22

### Changed

- Extracted the user-facing command tree wiring into [UserCommandTree.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/UserCommandTree.java)
- Continued Phase 1 command decomposition so `EconomyCommands` no longer owns the full user command registration tree directly
- Kept user handlers in `EconomyCommands` while moving user command structure into a dedicated file for the next refactor stage

## [1.2.5] - 2026-03-22

### Changed

- Extracted the admin and server command tree wiring into [AdminCommandTree.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/AdminCommandTree.java)
- Kept `EconomyCommands` as the owner of command handlers while moving admin/server registration into a dedicated command-tree class
- Continued Phase 1 roadmap work by turning command decomposition into a real file-level split

## [1.2.4] - 2026-03-22

### Added

- Added [ROADMAP.md](/Users/novaevent/Documents/CODEX/ZEconomy/ROADMAP.md) to track phased architecture, performance, testing, and release goals

### Changed

- Started Phase 1 implementation by splitting `EconomyCommands.register(...)` into thematic command registration helpers
- Separated user, admin utility, admin maintenance, server treasury, and exchange block command tree wiring to prepare the next step of extracting command classes

## [1.2.3] - 2026-03-22

### Changed

- Added `AdminReportService` to centralize admin status, export status, doctor, and player inspect report data
- Reduced direct runtime/config/report assembly inside `EconomyCommands` so command handlers stay closer to presentation logic

## [1.2.2] - 2026-03-22

### Changed

- Added `CurrencyAdminService` to centralize currency audit, inspect, summary, and repair workflows
- Reduced direct currency-state traversal inside `EconomyCommands` by moving diagnostic and repair logic into a shared service layer

## [1.2.1] - 2026-03-22

### Changed

- Added a shared `AdminOperationService` for treasury, reconcile, storage reload, doctor-fix, and Vault sync workflows
- Reduced command-layer duplication by moving admin refresh/save logic out of `EconomyCommands`

## [1.2.0] - 2026-03-22

### Changed

- Introduced a shared `EconomyOperationService` for core player economy operations
- Moved transfer, bank, vault, exchange, and daily reward workflows out of duplicated command/API paths

## [1.1.4] - 2026-03-22

### Changed

- Centralized player state refresh flows in shared helper methods
- Removed repeated manual `syncPlayerMirror` and `syncCustomData` sequences from commands, API, and network handlers

## [1.1.3] - 2026-03-22

### Changed

- Updated Forge bootstrap code to modern `FMLJavaModLoadingContext` constructor injection
- Replaced deprecated `ResourceLocation(namespace, path)` construction with `fromNamespaceAndPath(...)`

## [1.1.2] - 2026-03-22

### Changed

- Updated Forge for Minecraft `1.20.1` from `47.2.0` to `47.4.18`

## [1.1.1] - 2026-03-22

### Changed

- Updated item registry access to Forge `1.20.1` registry APIs
- Updated creative tab registration to avoid unchecked generic casts in Forge `RegisterEvent`

## [1.1.0] - 2026-03-22

### Changed

- Moved the Forge common config into `config/zeconomy/zeconomy-common.toml`
- Standardized generated GUI config files under `config/zeconomy/`

### Improved

- Added cached per-currency bank reservation totals to avoid repeated full rescans
- Batched multi-step balance updates to reduce repeated sync packets and Vault bridge writes
- Reduced hourly interest checks to a coarse scheduled sweep instead of scanning every tick
- Moved autosave writes to a background worker while keeping a fresh snapshot on the server thread

## [1.0.0] - 2026-03-22

Initial documented release.

### Added

- Forge mod bootstrap for Minecraft `1.20.1`
- Economy system with multiple currencies, wallets, banks, vaults, exchange, treasury, and mail
- Public Java API and integration example documentation
- Configurable storage backends including `nbt`, `json`, `sqlite`, and `mysql`
