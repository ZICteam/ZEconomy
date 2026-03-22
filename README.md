# ZEconomy

`ZEconomy` is an economy mod for `Minecraft Forge 1.20.1`.

It provides:

- multiple currencies
- player wallet balances
- bank deposits and hourly interest
- personal vault storage with PIN
- exchange rates and exchange operations
- mailbox item delivery
- server treasury account
- public Java API and Forge events for integrations
- Vault bridge support on hybrid servers such as `Arclight`, `Mohist`, and `CatServer`

## Features

- Core currencies: `z_coin` and `b_coin`
- Commands under `/zeco`
- Configurable storage backends:
  - `nbt`
  - `json`
  - `sqlite`
  - `mysql`
- Runtime GUI layout configs
- Export of economy snapshot data
- Integration-oriented API in `net.sixik.zeconomy.api`

## Requirements

- Minecraft `1.20.1`
- Forge `47.x`
- Java `17`

## Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Build output:

- `build/libs/Z_Economy.jar`

## Development Setup

IntelliJ IDEA:

```bash
./gradlew genIntellijRuns
```

Eclipse:

```bash
./gradlew genEclipseRuns
```

## Project Layout

- `src/main/java/net/sixik/zeconomy` - mod source
- `src/main/resources` - assets, recipes, metadata
- `examples/integration-example` - example addon integration
- `API.md` - public API overview and examples

## Public API

The stable integration surface is exposed through:

- `net.sixik.zeconomy.api.ZEconomyApi`
- `net.sixik.zeconomy.api.ZEconomyApiProvider`
- `net.sixik.zeconomy.api.event.*`

Quick example:

```java
var api = net.sixik.zeconomy.api.ZEconomyApiProvider.get();
var snapshot = api.getPlayerSnapshot(playerUuid);
```

See [API.md](/z:/My_mods/ZEconomy/API.md) for more examples.

## Commands

Main command root:

- `/zeco`

Examples:

- `/zeco balance`
- `/zeco bank info`
- `/zeco daily claim`
- `/zeco top z_coin`
- `/zeco server balance`

## Vault Bridge

Vault sync is available on hybrid servers with Bukkit plugin support.

Requirements:

- `Vault`
- a Vault economy provider such as `EssentialsX Economy`
- hybrid core like `Arclight`, `Mohist`, or `CatServer`

## License

See [LICENSE.txt](/z:/My_mods/ZEconomy/LICENSE.txt).
