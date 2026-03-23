package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.ZEconomy;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public final class EconomyTransactionContext {
    private final Set<UUID> touchedPlayers = new LinkedHashSet<>();
    private final List<Runnable> postCommitActions = new LinkedList<>();
    private MinecraftServer server;
    private boolean saveRequested;
    private boolean currencyDataSyncRequested;

    public EconomyTransactionContext() {
    }

    public EconomyTransactionContext(EconomyOperationEffects effects) {
        if (effects == null) {
            return;
        }
        this.touchedPlayers.addAll(effects.touchedPlayers());
        this.postCommitActions.addAll(effects.postCommitActions());
        this.server = effects.server();
        this.saveRequested = effects.saveRequested();
        this.currencyDataSyncRequested = effects.currencyDataSyncRequested();
    }

    public EconomyTransactionContext touchPlayer(ServerPlayer player) {
        if (player == null) {
            return this;
        }
        touchedPlayers.add(player.getUUID());
        if (server == null) {
            server = player.server;
        }
        return this;
    }

    public EconomyTransactionContext touchPlayer(UUID playerId) {
        if (playerId != null) {
            touchedPlayers.add(playerId);
        }
        return this;
    }

    public EconomyTransactionContext touchPlayers(ServerPlayer... players) {
        if (players == null) {
            return this;
        }
        for (ServerPlayer player : players) {
            touchPlayer(player);
        }
        return this;
    }

    public EconomyTransactionContext useCurrentServer() {
        if (server == null) {
            server = ServerLifecycleHooks.getCurrentServer();
        }
        return this;
    }

    public EconomyTransactionContext requestSave() {
        saveRequested = true;
        return this;
    }

    public EconomyTransactionContext requestCurrencyDataSync() {
        currencyDataSyncRequested = true;
        return this;
    }

    public EconomyTransactionContext afterCommit(Runnable action) {
        if (action != null) {
            postCommitActions.add(action);
        }
        return this;
    }

    Set<UUID> touchedPlayers() {
        return Set.copyOf(touchedPlayers);
    }

    boolean saveRequested() {
        return saveRequested;
    }

    boolean currencyDataSyncRequested() {
        return currencyDataSyncRequested;
    }

    int postCommitActionCount() {
        return postCommitActions.size();
    }

    public void commit() {
        MinecraftServer targetServer = server == null ? ServerLifecycleHooks.getCurrentServer() : server;
        for (UUID playerId : touchedPlayers) {
            EconomyOperationService.syncIfOnline(playerId);
        }
        if (currencyDataSyncRequested && targetServer != null) {
            CurrencyHelper.syncCurrencyData(targetServer);
        }
        if (saveRequested) {
            if (targetServer != null) {
                CurrencyHelper.scheduleSave(targetServer);
            }
        }
        for (Runnable action : postCommitActions) {
            try {
                action.run();
            } catch (Exception e) {
                ZEconomy.printStackTrace("Failed to run post-commit effect", e);
            }
        }
    }
}
