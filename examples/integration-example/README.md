# ZEconomy Integration Example

This folder contains a minimal example of how another Forge mod can integrate with `ZEconomy`.

What is included:

- `ExampleEconomyAddon.java` - shows how to obtain the API and subscribe to events
- `ExampleEconomyCommands.java` - shows how to expose simple commands using the API

This example is not wired into the main mod build. It is a reference/template for addon developers.

## What it demonstrates

- reading player snapshots
- reading treasury and runtime status
- changing balances through the public API
- listening to `BalanceChangeEvent` and `DailyRewardEvent`

## Expected usage

Copy the relevant classes into your own Forge mod and replace the package name.
