# Changelog

All notable changes to this project should be documented in this file.

The format is based on keeping entries grouped by released mod version.

## [1.4.51] - 2026-03-23

### Changed

- Extended failure/result helpers in [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) so `bank`, `vault`, and `daily` flows can preserve more domain context on failure as well as success
- Added regression coverage in [EconomyOperationFailureFactoriesTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationFailureFactoriesTest.java) for failure-side result contracts, including requested amounts and daily streak payloads
- Continued the roadmap cleanup by making service-flow result construction more symmetric between success and failure paths

## [1.4.50] - 2026-03-23

### Changed

- Added result-factory helpers in [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) so `transfer`, `bank`, `vault`, `exchange`, and `daily` outcomes can be constructed directly from operation plans and reward state
- Added regression coverage in [EconomyOperationResultFactoriesTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationResultFactoriesTest.java) for `plan -> result contract` mapping across the main money flows
- Continued the roadmap shift toward testable service flows by separating execution planning from result construction in the orchestration layer

## [1.4.49] - 2026-03-23

### Changed

- Extended [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) with explicit plan models for `vault deposit` and `vault withdraw`, bringing vault flows into the same orchestration style as bank, transfer, exchange, and daily reward
- Added regression coverage in [EconomyOperationVaultPlansTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationVaultPlansTest.java) for wallet-to-vault movement, vault-to-wallet movement, and non-negative remaining vault balance bookkeeping
- Continued the roadmap move toward a uniform transaction planning layer, where every major money flow is described by explicit, testable execution plans

## [1.4.48] - 2026-03-23

### Changed

- Extended [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) with explicit plan models for `bank deposit` and `daily claim`, so those flows now follow the same quote/plan/delta orchestration style as `transfer`, `bank withdraw`, and `exchange`
- Added regression coverage in [EconomyOperationExtendedPlansTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationExtendedPlansTest.java) for treasury-to-player daily reward movement and player-to-treasury bank deposit movement
- Continued the roadmap work toward a more uniform orchestration layer, where money movement plans are explicit, testable, and separate from runtime execution

## [1.4.47] - 2026-03-23

### Changed

- Introduced explicit service-flow plan models in [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) for `transfer`, `bank withdraw`, and `exchange`, combining domain quotes with the delta batches they execute
- Added regression coverage in [EconomyOperationPlansTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationPlansTest.java) to lock in the quote-plus-deltas orchestration contracts for those flows
- Continued the roadmap shift toward clearer orchestration objects so operation planning is increasingly separate from execution and easier to evolve safely

## [1.4.46] - 2026-03-23

### Changed

- Extracted explicit delta-plan helpers in [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) for `transfer`, `bank withdraw`, and `exchange` orchestration
- Added regression coverage in [EconomyOperationDeltaPlansTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationDeltaPlansTest.java) so the low-level movement of funds between player, treasury, and fee collector is now locked in by tests
- Continued the roadmap move toward more explicit, testable service flows by separating delta planning from the rest of operation orchestration

## [1.4.45] - 2026-03-23

### Changed

- Extracted pure service-flow calculation helpers in [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) for transfer quotes, exchange quotes, and bank-withdraw remaining-deposit math
- Added regression coverage in [EconomyOperationCalculationsTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationCalculationsTest.java) to lock in those calculation contracts without requiring `ServerPlayer` bootstrap
- Continued the roadmap work toward more testable service flows by moving domain math out of inline orchestration code and into explicit helper models

## [1.4.44] - 2026-03-23

### Changed

- Added facade-level regression coverage in [ExtraEconomyDataTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/ExtraEconomyDataTest.java) for serialize/deserialize, save/load round-trips, runtime logs, export timestamp state, bank/vault state, and interest-sweep scheduling
- Expanded the roadmap testing phase to the public `ExtraEconomyData` facade so the external state API is now protected in addition to the internal aggregate root and subsystem layers
- Continued tightening the persistence and state-contract safety net around the refactored economy runtime model

## [1.4.43] - 2026-03-23

### Changed

- Hardened [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) so `applyBalanceDeltas(...)` now prevalidates projected balances and rejects invalid multi-delta batches before mutating live balances
- Added regression coverage in [EconomyBalanceEngineTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyBalanceEngineTest.java) for grouped delta application, no-op filtering, and non-partial failure behavior
- Made [CurrencyHelper.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/utils/CurrencyHelper.java) tolerate unavailable Forge config values in vault-sync helpers, so low-level balance updates and dev/test runs no longer fail before config bootstrap
- Continued the roadmap transactional-core work by protecting the low-level balance engine that powers transfer, bank, exchange, and daily reward flows

## [1.4.42] - 2026-03-23

### Changed

- Added persistence-layer regression coverage in [CurrencyDataTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/currencies/data/CurrencyDataTest.java) and [CurrencyPlayerDataServerTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/currencies/data/CurrencyPlayerDataServerTest.java)
- Locked in core-currency bootstrap behavior, custom currency round-trip serialization, player default-balance initialization, and server player-currency snapshot serialization
- Continued the roadmap testing phase deeper into the data model so future refactors are less likely to break persistence and default currency provisioning

## [1.4.41] - 2026-03-23

### Changed

- Added runtime/reporting regression coverage in [EconomyRuntimeReadServicesTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyRuntimeReadServicesTest.java) for recent transaction logs, exchange-rate reads, and runtime snapshot fields
- Expanded the roadmap testing phase into admin/reporting read paths so log history, export timestamps, and rate-count metadata are now protected by automated checks
- Locked in the read-only service contracts used by runtime snapshots and reporting features after the recent service-layer refactors

## [1.4.40] - 2026-03-23

### Changed

- Added service-layer state regression coverage in [EconomyStateServicesTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyStateServicesTest.java) for bank deposits, hourly payout bookkeeping, vault balances/PINs, daily claim state, and player snapshot reads
- Expanded the roadmap testing phase from aggregate-root persistence into the shared read/mutation service contracts that bridge `ExtraEconomyData`, `EconomyStateRoot`, and the upper service layer
- Locked in the new state-service boundaries so future refactors are less likely to break bank/vault/daily reads after internal model changes

## [1.4.39] - 2026-03-23

### Changed

- Added aggregate-root regression coverage in [EconomyStateRootTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyStateRootTest.java) for bank reserve persistence, rate state, daily claim state, and hourly interest sweep scheduling
- Expanded the roadmap testing phase from isolated subsystems into state-root integration behavior, including write/read round-trips through the combined internal economy state model
- Locked in the bank reserve cache behavior across serialization so the optimized treasury spendable path remains protected by regression coverage

## [1.4.38] - 2026-03-23

### Changed

- Added transaction-pipeline regression coverage in [EconomyOperationEffectsTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationEffectsTest.java) and [EconomyTransactionContextTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyTransactionContextTest.java)
- Extended [EconomyTransactionContext.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyTransactionContext.java) with package-scoped testability accessors so the transaction/effects layer can be unit-tested without invoking live save/sync side effects
- Continued the roadmap testing phase by locking in the shared commit pipeline behavior that now underpins most economy operations

## [1.4.37] - 2026-03-23

### Changed

- Added higher-level result-model regression coverage in [EconomyOperationResultModelsTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationResultModelsTest.java) for `daily`, `mail send`, and `mail claim` service outcomes
- Extended [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) result wrappers with convenience accessors so commands and API callers can read failure and claimed-item count without manually unpacking nested operation state
- Continued the roadmap testing phase by locking in user-facing service result contracts, not just low-level policy/state behavior

## [1.4.36] - 2026-03-23

### Changed

- Extracted additional pure orchestration guards in [EconomyPolicyService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyPolicyService.java) and [EconomyValidationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyValidationService.java) for `daily claim` treasury eligibility and `mail send` validation precedence
- Updated [EconomyOperationService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) to use the new mail validation contract instead of manually chaining mailbox/self-target/held-item checks
- Expanded [EconomyPolicyServiceTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyPolicyServiceTest.java) and [EconomyValidationServiceTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyValidationServiceTest.java) with regression coverage for daily reward treasury guards and mail-send failure ordering

## [1.4.35] - 2026-03-23

### Changed

- Extracted pure policy helpers in [EconomyPolicyService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyPolicyService.java) for bank-withdraw and exchange eligibility so more domain guard logic can be regression-tested without heavy Forge runtime setup
- Expanded [EconomyPolicyServiceTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyPolicyServiceTest.java) with higher-level policy coverage for bank reserve checks, treasury spendable guards, exchange-rate validity, wallet insufficiency, and treasury target-liquidity scenarios
- Continued the roadmap testing phase by making more policy logic explicitly testable and locking in key failure modes around treasury-backed operations

## [1.4.34] - 2026-03-23

### Changed

- Added higher-level regression coverage for [EconomyPolicyServiceTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyPolicyServiceTest.java), [EconomyOperationServiceRateTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyOperationServiceRateTest.java), and [EconomyValidationServiceTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/EconomyValidationServiceTest.java)
- Expanded the testing phase from bounded-context state checks into policy and operation behavior, including daily streak progression, rate reset behavior, and PIN validation rules
- Continued building a lightweight regression net around the service-layer domain rules after the recent architectural refactors

## [1.4.33] - 2026-03-23

### Changed

- Added regression coverage for [RateSubsystemTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/RateSubsystemTest.java) and [TransactionLogSubsystemTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/TransactionLogSubsystemTest.java)
- Expanded the testing phase beyond bank/vault/daily flows so the newly extracted rate and log bounded contexts also have direct automated checks
- Continued building a lightweight regression net around the refactored internal state model before moving deeper into higher-level policy and operation tests

## [1.4.32] - 2026-03-23

### Changed

- Added a lightweight JUnit 5 test setup in [build.gradle](/Users/novaevent/Documents/CODEX/ZEconomy/build.gradle) so the refactored domain/state layers can start accumulating fast regression coverage
- Added first bounded-context regression tests for [BankStateFacadeTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/BankStateFacadeTest.java), [VaultStateFacadeTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/VaultStateFacadeTest.java), and [DailyRewardFacadeTest.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/test/java/io/zicteam/zeconomy/system/DailyRewardFacadeTest.java)
- Started the roadmap testing phase by locking in key bank/vault/daily state transitions after the recent bounded-context refactors

## [1.4.31] - 2026-03-23

### Changed

- Added bounded-context state facades for the main economy domains: [BankStateFacade.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/BankStateFacade.java), [VaultStateFacade.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/VaultStateFacade.java), and [DailyRewardFacade.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/DailyRewardFacade.java)
- Switched [EconomyStateRoot.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyStateRoot.java) to compose those domain-specific facades instead of talking directly to the low-level bank, vault, and daily state classes
- Continued the roadmap move toward clearer bounded contexts inside the internal state model, so the aggregate root now reads more like a composition of domain zones than a flat bag of subsystem fields

## [1.4.30] - 2026-03-23

### Changed

- Introduced [EconomyStateRoot.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyStateRoot.java) as the formal aggregate root for economy subsystem state, sweep scheduling metadata, and NBT serialization
- Switched [ExtraEconomyData.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExtraEconomyData.java) to delegate state ownership, reads, writes, and load/save serialization through that root object instead of owning all subsystem fields directly
- Continued the roadmap transition from a wide universal state holder toward a thinner facade over a more explicit internal state model

## [1.4.29] - 2026-03-23

### Changed

- Added [EconomyPlayerSyncService.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyPlayerSyncService.java) to centralize derived player mirror/custom-data synchronization instead of building that state directly inside `ExtraEconomyData`
- Switched [CurrencyHelper.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/utils/CurrencyHelper.java) and [ZEconomyEvents.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/ZEconomyEvents.java) onto the new sync service for player refresh and login/vault-sync mirror updates
- Continued thinning [ExtraEconomyData.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExtraEconomyData.java) by removing its direct mirror-sync orchestration responsibility

## [1.4.28] - 2026-03-23

### Changed

- Extracted remaining rate, mailbox, and transaction-log responsibilities from [ExtraEconomyData.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExtraEconomyData.java) into dedicated subsystem facades: [RateSubsystem.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/RateSubsystem.java), [MailboxSubsystem.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/MailboxSubsystem.java), and [TransactionLogSubsystem.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/TransactionLogSubsystem.java)
- Switched `ExtraEconomyData` to delegate rate queries/mutations, mailbox state, log state, and their NBT serialization through those narrower subsystem facades instead of holding the raw state classes directly
- Continued the roadmap transition of `ExtraEconomyData` into a thin aggregator over specialized subdomains rather than a broad container of unrelated runtime concerns

## [1.4.27] - 2026-03-23

### Changed

- Extracted hourly bank-interest orchestration into [EconomyInterestService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyInterestService.java), so the server tick no longer drives that background economy flow through procedural logic inside `ExtraEconomyData`
- Added explicit hourly-interest state access helpers to [EconomySnapshotReadService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomySnapshotReadService.java), [EconomyStateMutationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyStateMutationService.java), and [ExtraEconomyData.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExtraEconomyData.java) for sweep scheduling and bank payout bookkeeping
- Continued shrinking `ExtraEconomyData` toward a state-focused aggregator by removing one more remaining runtime orchestration hotspot from the class

## [1.4.26] - 2026-03-23

### Changed

- Added [EconomyRateMutationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyRateMutationService.java) and [EconomyLogService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyLogService.java) to centralize rate mutations and transaction-log writes behind dedicated service contracts
- Switched [EconomyOperationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java), [AdminOperationService](/Users/novaevent/Documents/CODEX/ZEconomY/src/main/java/io/zicteam/zeconomy/system/AdminOperationService.java), [ZEconomyEvents.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/ZEconomyEvents.java), [EconomyCommands.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/EconomyCommands.java), [UserEconomyCommands.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/UserEconomyCommands.java), and [ZEconomyApiImpl.java](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/api/ZEconomyApiImpl.java) onto these new rate/log and rate-read paths
- Continued narrowing `ExtraEconomyData` by moving more operational entry points for rates and logs into explicit subsystem services instead of using the facade directly from upper layers

## [1.4.25] - 2026-03-23

### Changed

- Added [EconomyStateMutationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyStateMutationService.java) to centralize state mutations for bank deposits, vault balances, daily-claim state, vault PIN updates, and mailbox sends/claims
- Switched [EconomyOperationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java) to use those dedicated mutation contracts instead of writing state through `ZEconomy.EXTRA_DATA` directly
- Continued the roadmap transition toward explicit state-specific contracts, so orchestration code now depends less on the broad `ExtraEconomyData` facade for internal mutations

## [1.4.24] - 2026-03-23

### Changed

- Expanded [EconomySnapshotReadService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomySnapshotReadService.java) with domain-level read helpers for bank deposits, vault balances/PIN checks, daily state, treasury spendable, exchange rates, and runtime counters
- Switched [EconomyPolicyService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyPolicyService.java), [EconomyValidationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyValidationService.java), [EconomyOperationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyOperationService.java), [AdminOperationService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/AdminOperationService.java), and [EconomyReadService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyReadService.java) to consume those read contracts instead of reading `ZEconomy.EXTRA_DATA` fields directly
- Continued the roadmap cleanup by tightening the boundary between orchestration, validation, and policy code versus the state facade, so direct `EXTRA_DATA` reads are increasingly centralized in dedicated read services

## [1.4.23] - 2026-03-23

### Changed

- Added [EconomySnapshotReadService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomySnapshotReadService.java) to centralize player snapshot, runtime metric, and recent-log reads instead of having commands, API, and report builders pull those fields directly from `ExtraEconomyData`
- Switched [AdminReportService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/AdminReportService.java), [ZEconomyApiImpl](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/api/ZEconomyApiImpl.java), [EconomyCommands](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/EconomyCommands.java), [AdminMaintenanceCommands](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/AdminMaintenanceCommands.java), [UserEconomyCommands](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/commands/UserEconomyCommands.java), and [EconomyExportService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyExportService.java) onto that read layer
- Continued narrowing upper-layer dependence on `ZEconomy.EXTRA_DATA`, keeping facade-style state access behind dedicated report/read services

## [1.4.22] - 2026-03-23

### Changed

- Extracted export scheduling and JSON export generation into [EconomyExportService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyExportService.java), so server startup, tick-based export, and admin/manual export no longer go through procedural methods on `ExtraEconomyData`
- Extracted top-rich reporting into [EconomyReadService](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/EconomyReadService.java) and switched command/API callers to use that dedicated read layer
- Reduced `ExtraEconomyData` further toward a state facade by removing its export/runtime reporting implementation and leaving only export scheduling state plus log/state accessors

## [1.4.21] - 2026-03-23

### Changed

- Extracted exchange-rate storage into [ExchangeRateState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExchangeRateState.java) and export timing metadata into [ExportRuntimeState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/ExportRuntimeState.java)
- Switched `ExtraEconomyData` rate queries/mutations and export scheduling metadata to delegate through those dedicated state holders
- Continued shrinking `ExtraEconomyData` into a facade over focused subsystem state instead of storing unrelated maps and runtime fields directly

## [1.4.20] - 2026-03-23

### Changed

- Extracted mailbox storage into [MailboxState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/MailboxState.java) and transaction log storage into [TransactionLogState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/TransactionLogState.java)
- Switched `ExtraEconomyData` mail and transaction-log methods plus NBT serialization to delegate through those dedicated state holders
- Continued the roadmap transition of `ExtraEconomyData` from a wide state monolith into a facade over smaller domain-specific state components

## [1.4.19] - 2026-03-23

### Changed

- Extracted vault balances/PIN state into [VaultLedgerState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/VaultLedgerState.java) and daily reward claim state into [DailyRewardState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/DailyRewardState.java)
- Switched `ExtraEconomyData` vault and daily query/mutation methods to delegate through those dedicated state objects instead of owning the raw maps directly
- Continued narrowing `ExtraEconomyData` toward a facade over smaller state holders plus export/log/mail concerns, instead of one wide state container for every economy subsystem

## [1.4.18] - 2026-03-23

### Changed

- Extracted bank deposit, reserved-total cache, and hourly payout timestamp state into [BankLedgerState](/Users/novaevent/Documents/CODEX/ZEconomy/src/main/java/io/zicteam/zeconomy/system/BankLedgerState.java), reducing how much raw bank bookkeeping lives directly in `ExtraEconomyData`
- Switched `ExtraEconomyData` bank queries, hourly interest bookkeeping, and bank-state mutation helpers to delegate through the new bank ledger state object
- Removed the leftover no-op cache rebuild path from `ExtraEconomyData`, since the bank cache is now rebuilt inside the dedicated ledger state class

## [1.4.17] - 2026-03-23

### Changed

- Moved `transfer` and `exchange` orchestration fully into `EconomyOperationService`, so those interactive economy paths now execute entirely through service-owned balance-delta and commit-effect logic
- Removed the now-obsolete `transferWithFee(...)` and `exchangeCurrency(...)` orchestration methods from `ExtraEconomyData`
- Continued shrinking `ExtraEconomyData` toward a state-focused data holder instead of a second transaction service

## [1.4.16] - 2026-03-23

### Changed

- Moved bank deposit/withdraw, vault deposit/withdraw, and daily-claim orchestration out of `ExtraEconomyData` and into `EconomyOperationService`
- Added explicit state-only helpers in `ExtraEconomyData` for bank, vault, and daily reward mutation state so the data layer is more clearly separated from validation, balance-delta orchestration, and commit effects
- Continued reducing `ExtraEconomyData` as a parallel service layer, with interactive economy flows now leaning more directly on service-owned transaction logic

## [1.4.15] - 2026-03-23

### Changed

- Moved hourly bank-interest refresh, transaction logging, and interest-event dispatch onto `EconomyOperationEffects`, so this background sweep now uses the same commit pipeline as the rest of the service-layer side effects
- Replaced direct in-loop mirror refresh in the hourly interest pass with touched-player effect dispatch, keeping player state updates aligned with the shared transaction refresh flow
- Reduced one more orchestration hotspot inside `ExtraEconomyData`, bringing the background economy sweep closer to the same side-effect model as interactive economy operations

## [1.4.14] - 2026-03-23

### Changed

- Removed hidden player-refresh orchestration from the main `ExtraEconomyData` mutation methods for transfer, bank, vault, exchange, and daily reward flows
- Kept these operations aligned with the shared transaction pipeline, so touched-player refresh now consistently comes from service-layer commit dispatch instead of being split between service and storage classes
- Narrowed `ExtraEconomyData` further toward pure state mutation, leaving hourly-interest mirror maintenance as the remaining notable in-class refresh path

## [1.4.13] - 2026-03-23

### Changed

- Moved transfer, bank, vault, exchange, and daily transaction logging plus their domain API events out of direct `ExtraEconomyData` mutations and into `EconomyOperationService` post-commit effects
- Switched bank and exchange network actions onto the shared economy service path so GUI-triggered operations no longer bypass transactional log/event dispatch
- Reduced another layer of hidden side effects inside `ExtraEconomyData`, keeping the domain storage class more focused on state mutation while the service layer orchestrates commit-time outcomes

## [1.4.12] - 2026-03-23

### Changed

- Added post-commit callback support to `EconomyOperationEffects` and `EconomyTransactionContext`, so service-layer operations can now dispatch structured event/log side effects through the same transaction pipeline as sync and save work
- Moved rate-change events, vault PIN set events, and mailbox transaction logging out of direct `ExtraEconomyData` mutation methods and into `EconomyOperationService` post-commit effects
- Switched mailbox network actions onto the shared mail service path so GUI-driven mailbox send/claim flows no longer bypass the transactional service layer

## [1.4.11] - 2026-03-23

### Changed

- Extended `EconomyOperationEffects` and `EconomyTransactionContext` with server-wide currency-data sync support so commit dispatch can now coordinate registry syncs in addition to player refresh and scheduled saves
- Switched storage reload and currency admin flows to the shared effect dispatch path instead of manually mixing `syncCurrencyData`, player sync, and save scheduling in commands and services
- Updated currency repair and lock workflows to batch touched-player refresh through the common transaction/effects layer, reducing more ad hoc post-mutation orchestration

## [1.4.10] - 2026-03-23

### Changed

- Aligned `AdminOperationService` result models with `EconomyOperationEffects`, so admin balance, treasury, doctor-fix, reconcile, and Vault sync flows now carry the same post-commit effect metadata as economy-side operations
- Removed more ad hoc admin-side post-success refresh/save wiring by routing reconcile, doctor-fix, and successful Vault sync through the shared transaction-style effect dispatch path
- Continued converging the admin layer toward the same validation, commit, and result contract used by the transactional economy core

## [1.4.9] - 2026-03-23

### Changed

- Added `EconomyOperationEffects` as a structured set of post-commit side effects for successful economy operations
- Connected `EconomyOperationService` results to explicit effect payloads so transaction-style outcomes can now carry touched-player sync and save intent alongside domain data
- Linked `EconomyOperationEffects` with `EconomyTransactionContext` to keep commit behavior reusable while making operation results more self-describing

## [1.4.8] - 2026-03-23

### Changed

- Added `EconomyTransactionContext` to centralize touched-player refresh and scheduled persistence after successful economy mutations
- Switched economy and admin post-success paths away from scattered `afterSuccess(...)` helpers toward a shared transaction-style commit object
- Prepared the service layer for future unified effect dispatch by separating commit behavior from operation validation and policy decisions

## [1.4.7] - 2026-03-23

### Changed

- Added `EconomyPolicyService` as a higher-level home for domain eligibility rules such as transfer, bank, exchange, vault access, and daily claim policies
- Switched core economy operations to consume shared policy decisions instead of inlining business-rule orchestration in each service method
- Added read-only state helpers in `ExtraEconomyData` so policy checks can query vault and daily state without reaching into internal maps directly

## [1.4.6] - 2026-03-23

### Changed

- Added `EconomyValidationService` as a shared home for economy-side invariants such as amount, pin, mailbox, rate, wallet, and treasury-spendable checks
- Switched core economy and admin operation services to use the shared validation layer instead of scattering duplicated guard logic across multiple methods
- Prepared the service layer for a cleaner next step toward explicit domain policies by separating validation rules from mutation orchestration

## [1.4.5] - 2026-03-23

### Changed

- Added service-layer operations for vault PIN, mailbox send/claim, and exchange rate mutations
- Moved user commands and API paths for mailbox, vault pin, and rate management off direct `ExtraEconomyData` mutations onto structured `EconomyOperationService` results
- Extended operation failure coverage so more economy-side workflows now report consistent domain errors through the shared service layer

## [1.4.4] - 2026-03-23

### Changed

- Added domain-specific result models in `EconomyOperationService` for transfer, bank, vault, and exchange flows
- Updated command feedback to consume structured operation results directly instead of recalculating fees, rates, and resulting balances outside the service layer
- Updated API economy operations to return values from domain results rather than rereading mutable state after each call

## [1.4.3] - 2026-03-22

### Changed

- Added shared `BalanceDelta` batching to `EconomyOperationService` for grouped wallet mutations
- Switched exchange, transfer, bank, vault, daily reward, and hourly interest internals in `ExtraEconomyData` to use the shared balance-delta helper instead of repeated direct `addCurrencyValue(...)` sequences
- Reduced low-level wallet mutation sprawl inside domain operations, preparing the next step toward more explicit transactional commit paths

## [1.4.2] - 2026-03-22

### Changed

- Added structured balance-mutation results to `EconomyOperationService` for direct `addBalance` and `setBalance` flows
- Switched API direct balance updates and the Impactor currency bridge to use the shared balance-mutation service path instead of calling `CurrencyPlayerData` mutations directly
- Routed admin player balance updates through the same service contract so direct wallet edits now share the same sync and persistence lifecycle as the rest of the transactional core

## [1.4.1] - 2026-03-22

### Changed

- Extended the transactional-style service layer to admin balance, treasury, and Vault sync flows through structured `AdminOperationService` results
- Moved admin post-success persistence scheduling into the service layer so commands and API no longer duplicate save-queue logic after treasury or balance mutations
- Updated command and API treasury paths to consume admin operation failure reasons directly instead of rebuilding validation and persistence behavior ad hoc

## [1.4.0] - 2026-03-22

### Changed

- Introduced structured `OperationResult` and `DailyOperationResult` responses in `EconomyOperationService` so transfer, bank, vault, exchange, and daily flows no longer rely on opaque boolean outcomes
- Centralized the shared post-success path for user economy operations so successful commits now consistently refresh player state and queue persistence through one service-layer hook
- Updated command and API layers to consume operation failure reasons directly, improving consistency between gameplay feedback and external API error mapping

## [1.3.5] - 2026-03-22

### Improved

- Switched non-critical admin mutation flows from immediate `saveAll(...)` flushes to `scheduleSave(...)` so manual repair and bulk lock operations stop blocking the main thread unnecessarily
- Added scheduled persistence after admin balance, treasury, currency create/delete, and Vault sync actions so operator changes are queued for save immediately instead of waiting only for autosave
- Kept explicit `save`, `backup`, `reload`, and shutdown flows on synchronous persistence to preserve their stronger consistency guarantees

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
