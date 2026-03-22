package io.zicteam.zeconomy.api.model;

public record RuntimeStatusView(
    boolean vaultBridgeEnabled,
    boolean vaultBridgeAvailable,
    String vaultProvider,
    String vaultSyncCurrencyId,
    int pendingVaultSyncCount,
    String storageMode,
    int onlinePlayers,
    int logCount,
    long lastExportEpochSec
) {
}
