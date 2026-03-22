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
- Integration-oriented API in `io.zicteam.zeconomy.api`

## Requirements

- Minecraft `1.20.1`
- Forge `47.x`
- Java `17`

## Installation

1. Build the mod or take the packaged jar from `build/libs/Z_Economy.jar`.
2. Put the jar into the server `mods` folder.
3. Start the server once to generate config files.
4. Edit the generated config if needed.
5. Restart the server fully after config changes.

Main config locations:

- `config/zeconomy-common.toml`
- `world/serverconfig/zeconomy`

For hybrid servers with plugin support:

- install `Vault`
- install a Vault economy provider such as `EssentialsX Economy`
- enable the Vault bridge in the config

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

- `src/main/java/io/zicteam/zeconomy` - mod source
- `src/main/resources` - assets, recipes, metadata
- `examples/integration-example` - example addon integration
- `API.md` - public API overview

## Public API

The stable integration surface is exposed through:

- `io.zicteam.zeconomy.api.ZEconomyApi`
- `io.zicteam.zeconomy.api.ZEconomyApiProvider`
- `io.zicteam.zeconomy.api.event.*`

Quick example:

```java
var api = io.zicteam.zeconomy.api.ZEconomyApiProvider.get();
var snapshot = api.getPlayerSnapshot(playerUuid);
```

See [API.md](/z:/My_mods/ZEconomy/API.md) for the API surface and [examples/integration-example/README.md](/z:/My_mods/ZEconomy/examples/integration-example/README.md) for addon-oriented examples.

## Commands

Main command root:

- `/zeco`

Examples:

- `/zeco balance`
- `/zeco balance <player>`
- `/zeco pay <player> <currency> <amount>`
- `/zeco bank info`
- `/zeco bank deposit <currency> <amount>`
- `/zeco bank withdraw <currency> <amount>`
- `/zeco exchange <from> <to> <amount>`
- `/zeco exchange rates`
- `/zeco mail claim`
- `/zeco mail send <player>`
- `/zeco vault setpin <pin>`
- `/zeco vault balance`
- `/zeco vault deposit <pin> <currency> <amount>`
- `/zeco vault withdraw <pin> <currency> <amount>`
- `/zeco daily claim`
- `/zeco top z_coin`
- `/zeco currencies`
- `/zeco status`
- `/zeco server balance`

Admin commands:

- `/zeco reload`
- `/zeco export now`
- `/zeco logs [limit]`
- `/zeco gui edit <target>`
- `/zeco admin set <player> <currency> <amount>`
- `/zeco admin currency ...`
- `/zeco admin status`
- `/zeco admin save`
- `/zeco admin backup`
- `/zeco admin reloadstorage`
- `/zeco admin exportnow`
- `/zeco admin exportstatus`
- `/zeco admin doctor`
- `/zeco admin doctorfix <player>`
- `/zeco admin doctorfixall`
- `/zeco admin inspect <player>`
- `/zeco admin reconcile <player>`
- `/zeco admin syncvault <player>`
- `/zeco server set <currency> <amount>`
- `/zeco server give <currency> <amount>`
- `/zeco server take <currency> <amount>`
- `/zeco exchangeblock set <pos> <input_item> <input_count> <output_item> <output_count>`

## Storage

Supported storage modes:

- `nbt`
- `json`
- `sqlite`
- `mysql`

The configured storage mode controls where runtime data is written under `world/serverconfig/zeconomy`.

## Vault Bridge

Vault sync is available on hybrid servers with Bukkit plugin support.

Requirements:

- `Vault`
- a Vault economy provider such as `EssentialsX Economy`
- hybrid core like `Arclight`, `Mohist`, or `CatServer`

## License

See [LICENSE.txt](/z:/My_mods/ZEconomy/LICENSE.txt).
