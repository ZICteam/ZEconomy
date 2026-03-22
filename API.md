# ZEconomy API

`ZEconomy` exposes a public Java API under `io.zicteam.zeconomy.api`.

## Entry Point

Use the provider to obtain the active API instance:

```java
import io.zicteam.zeconomy.api.ZEconomyApi;
import io.zicteam.zeconomy.api.ZEconomyApiProvider;

ZEconomyApi api = ZEconomyApiProvider.get();
```

## Main Interface

The public contract is defined in [ZEconomyApi.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/ZEconomyApi.java).

Read methods:

- `getInfo()`
- `getCurrencies()`
- `getCurrency(String currencyId)`
- `getPlayerSnapshot(UUID playerId)`
- `getTreasurySnapshot(String currencyId)`
- `getTreasurySnapshots()`
- `getRates()`
- `getRate(String fromCurrencyId, String toCurrencyId)`
- `getTopRich(String currencyId, int limit, boolean includeVault)`
- `getRecentLogs(int limit)`
- `getRuntimeStatus()`

Mutation methods:

- `addBalance(UUID playerId, String currencyId, double amount)`
- `setBalance(UUID playerId, String currencyId, double amount)`
- `transfer(UUID fromPlayerId, UUID toPlayerId, String currencyId, double amount)`
- `depositBank(UUID playerId, String currencyId, double amount)`
- `withdrawBank(UUID playerId, String currencyId, double amount)`
- `setVaultPin(UUID playerId, String pin)`
- `depositVault(UUID playerId, String pin, String currencyId, double amount)`
- `withdrawVault(UUID playerId, String pin, String currencyId, double amount)`
- `claimDaily(UUID playerId)`
- `exchange(UUID playerId, String fromCurrencyId, String toCurrencyId, double amount)`
- `setTreasuryBalance(String currencyId, double amount)`
- `addTreasuryBalance(String currencyId, double amount)`
- `takeTreasuryBalance(String currencyId, double amount)`
- `setRate(String fromCurrencyId, String toCurrencyId, double rate)`
- `removeRate(String fromCurrencyId, String toCurrencyId)`
- `resetRates()`

## Result Model

Most methods return `ApiResult<T>`.

Use it to inspect:

- success state
- returned payload
- error code
- message text

Important model types:

- [PlayerEconomySnapshot.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/PlayerEconomySnapshot.java)
- [TreasurySnapshot.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/TreasurySnapshot.java)
- [RuntimeStatusView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/RuntimeStatusView.java)
- [CurrencyView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/CurrencyView.java)
- [RateView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/RateView.java)
- [RichEntryView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/RichEntryView.java)
- [TransactionLogView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/TransactionLogView.java)
- [DailyRewardView.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/model/DailyRewardView.java)

## Snapshot Data

`PlayerEconomySnapshot` contains:

- player UUID and player name
- wallet balances by currency
- bank balances by currency
- vault balances by currency
- pending mail count
- daily streak
- daily-claimed state
- whether the player has a vault PIN

`TreasurySnapshot` contains:

- currency id
- total treasury balance
- reserved balance
- spendable balance

`RuntimeStatusView` contains:

- whether the Vault bridge is enabled
- whether the Vault bridge is currently available
- active Vault provider name
- configured sync currency id
- pending Vault sync queue size
- active storage mode
- online player count
- current log count
- last export timestamp

## Events

`ZEconomy` posts Forge events for integrations:

- [BalanceChangeEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/BalanceChangeEvent.java)
- [BankTransactionEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/BankTransactionEvent.java)
- [VaultTransactionEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/VaultTransactionEvent.java)
- [ExchangeEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/ExchangeEvent.java)
- [DailyRewardEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/DailyRewardEvent.java)
- [RateChangeEvent.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/RateChangeEvent.java)

Helper class:

- [ZEconomyApiEvents.java](/z:/My_mods/ZEconomy/src/main/java/io/zicteam/zeconomy/api/event/ZEconomyApiEvents.java)

## Example Usage

Read player state:

```java
var snapshotResult = api.getPlayerSnapshot(playerUuid);
if (snapshotResult.success()) {
    var snapshot = snapshotResult.data();
    double wallet = snapshot.walletBalances().getOrDefault("z_coin", 0.0D);
}
```

Give currency:

```java
var result = api.addBalance(playerUuid, "z_coin", 250.0D);
if (!result.success()) {
    System.out.println(result.errorCode() + ": " + result.message());
}
```

Read treasury state:

```java
var treasuryResult = api.getTreasurySnapshot("z_coin");
if (treasuryResult.success()) {
    var treasury = treasuryResult.data();
    System.out.println(treasury.spendableBalance());
}
```

Subscribe to events:

```java
MinecraftForge.EVENT_BUS.addListener((BalanceChangeEvent event) -> {
    System.out.println(event.currencyId() + " changed for " + event.playerId());
});
```

## Integration Notes

- Use the public API package, not internal `system`, `utils`, or `data` classes.
- Check `ApiResult` before using the payload.
- Treat currency ids such as `z_coin` and `b_coin` as runtime data, not hardcoded assumptions unless your addon explicitly depends on them.
- On hybrid servers, Vault-related state depends on the Bukkit plugin layer being available.
