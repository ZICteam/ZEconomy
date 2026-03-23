package io.zicteam.zeconomy.system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class EconomyOperationEffects {
    private final Set<UUID> touchedPlayers = new LinkedHashSet<>();
    private final List<Runnable> postCommitActions = new ArrayList<>();
    private MinecraftServer server;
    private boolean saveRequested;
    private boolean currencyDataSyncRequested;

    public static EconomyOperationEffects none() {
        return new EconomyOperationEffects();
    }

    public EconomyOperationEffects touchPlayer(ServerPlayer player) {
        if (player != null) {
            touchedPlayers.add(player.getUUID());
            if (server == null) {
                server = player.server;
            }
        }
        return this;
    }

    public EconomyOperationEffects touchPlayer(UUID playerId) {
        if (playerId != null) {
            touchedPlayers.add(playerId);
        }
        return this;
    }

    public EconomyOperationEffects touchPlayers(ServerPlayer... players) {
        if (players == null) {
            return this;
        }
        for (ServerPlayer player : players) {
            touchPlayer(player);
        }
        return this;
    }

    public EconomyOperationEffects useServer(MinecraftServer server) {
        if (server != null) {
            this.server = server;
        }
        return this;
    }

    public EconomyOperationEffects useCurrentServer() {
        if (server == null) {
            server = ServerLifecycleHooks.getCurrentServer();
        }
        return this;
    }

    public EconomyOperationEffects requestSave() {
        saveRequested = true;
        return this;
    }

    public EconomyOperationEffects requestCurrencyDataSync() {
        currencyDataSyncRequested = true;
        return this;
    }

    public EconomyOperationEffects afterCommit(Runnable action) {
        if (action != null) {
            postCommitActions.add(action);
        }
        return this;
    }

    public Set<UUID> touchedPlayers() {
        return Collections.unmodifiableSet(touchedPlayers);
    }

    public MinecraftServer server() {
        return server;
    }

    public boolean saveRequested() {
        return saveRequested;
    }

    public boolean currencyDataSyncRequested() {
        return currencyDataSyncRequested;
    }

    public List<Runnable> postCommitActions() {
        return List.copyOf(postCommitActions);
    }

    public void dispatch() {
        new EconomyTransactionContext(this).commit();
    }
}
