# Changelog

All notable changes to this project should be documented in this file.

The format is based on keeping entries grouped by released mod version.

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
