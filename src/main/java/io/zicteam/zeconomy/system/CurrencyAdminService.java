package io.zicteam.zeconomy.system;

import io.zicteam.zeconomy.currencies.BaseCurrency;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CurrencyAdminService {
    private CurrencyAdminService() {
    }

    public static CurrencyAuditResult auditCurrencies() {
        Set<String> registryCurrencies = registryCurrencyIds();
        int players = 0;
        int missingEntries = 0;
        int danglingEntries = 0;
        for (Map.Entry<UUID, LinkedList<CurrencyPlayerData.PlayerCurrency>> entry : CurrencyPlayerData.SERVER.playersCurrencyMap.entrySet()) {
            players++;
            Set<String> playerCurrencies = entry.getValue().stream()
                .map(pc -> pc.currency.getName())
                .collect(Collectors.toSet());
            for (String currencyId : registryCurrencies) {
                if (!playerCurrencies.contains(currencyId)) {
                    missingEntries++;
                }
            }
            for (CurrencyPlayerData.PlayerCurrency playerCurrency : entry.getValue()) {
                if (!registryCurrencies.contains(playerCurrency.currency.getName())) {
                    danglingEntries++;
                }
            }
        }
        return new CurrencyAuditResult(registryCurrencies.size(), players, missingEntries, danglingEntries);
    }

    public static CurrencyInspectResult inspectPlayer(ServerPlayer player) {
        CurrencyPlayerData.SERVER.newPlayer(player);
        return inspectEntries(CurrencyPlayerData.SERVER.getPlayersCurrency(player));
    }

    public static CurrencyInspectResult inspectPlayer(UUID playerId) {
        return inspectEntries(CurrencyPlayerData.SERVER.getPlayersCurrency(playerId));
    }

    public static CurrencyRepairResult repairPlayer(ServerPlayer player, boolean saveAfter) {
        CurrencyPlayerData.SERVER.newPlayer(player);
        CurrencyRepairResult result = repairEntries(player.getUUID(), CurrencyPlayerData.SERVER.getPlayersCurrency(player));
        EconomyOperationEffects effects = new EconomyOperationEffects().touchPlayer(player);
        if (saveAfter && player.server != null) {
            effects.useServer(player.server).requestSave();
        }
        effects.dispatch();
        return result;
    }

    public static Set<UUID> collectTargetIds(MinecraftServer server) {
        Set<UUID> targetIds = new LinkedHashSet<>(CurrencyPlayerData.SERVER.playersCurrencyMap.keySet());
        if (server == null) {
            return targetIds;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            CurrencyPlayerData.SERVER.newPlayer(player);
            targetIds.add(player.getUUID());
        }
        return targetIds;
    }

    public static CurrencyPlayersReport playerCoverage(MinecraftServer server, String currencyId) {
        Set<UUID> targetIds = collectTargetIds(server);
        int present = 0;
        int missing = 0;
        int locked = 0;
        LinkedList<CurrencyPlayerState> states = new LinkedList<>();
        for (UUID playerId : targetIds) {
            Optional<CurrencyPlayerData.PlayerCurrency> entry = CurrencyPlayerData.SERVER.getPlayerCurrency(playerId, currencyId);
            if (entry.isPresent()) {
                present++;
                CurrencyPlayerData.PlayerCurrency playerCurrency = entry.get();
                if (playerCurrency.isLocked) {
                    locked++;
                }
                states.add(new CurrencyPlayerState(playerId, true, playerCurrency.balance, playerCurrency.isLocked));
            } else {
                missing++;
                states.add(new CurrencyPlayerState(playerId, false, 0.0D, false));
            }
        }
        return new CurrencyPlayersReport(targetIds.size(), present, missing, locked, states);
    }

    public static CurrencyIssuesReport inspectIssues(MinecraftServer server) {
        Set<UUID> targetIds = collectTargetIds(server);
        int checked = 0;
        LinkedList<PlayerIssue> issues = new LinkedList<>();
        for (UUID playerId : targetIds) {
            CurrencyInspectResult result = inspectPlayer(playerId);
            checked++;
            if (result.missing() == 0 && result.dangling() == 0) {
                continue;
            }
            issues.add(new PlayerIssue(playerId, result.entries(), result.missing(), result.dangling(), result.locked()));
        }
        return new CurrencyIssuesReport(checked, issues);
    }

    public static LinkedList<CurrencySummaryLine> summarizeCurrencies(MinecraftServer server) {
        Set<UUID> targetIds = collectTargetIds(server);
        LinkedList<CurrencySummaryLine> lines = new LinkedList<>();
        for (BaseCurrency currency : CurrencyData.SERVER.currencies) {
            int present = 0;
            int missing = 0;
            int locked = 0;
            for (UUID playerId : targetIds) {
                Optional<CurrencyPlayerData.PlayerCurrency> entry = CurrencyPlayerData.SERVER.getPlayerCurrency(playerId, currency.getName());
                if (entry.isPresent()) {
                    present++;
                    if (entry.get().isLocked) {
                        locked++;
                    }
                } else {
                    missing++;
                }
            }
            lines.add(new CurrencySummaryLine(currency.getName(), present, missing, locked));
        }
        return lines;
    }

    public static CurrencyBatchRepairResult repairIssues(MinecraftServer server) {
        Set<UUID> targetIds = collectTargetIds(server);
        int repairedPlayers = 0;
        int added = 0;
        int removed = 0;
        EconomyOperationEffects effects = new EconomyOperationEffects();
        if (server != null) {
            effects.useServer(server).requestSave();
        }
        for (UUID playerId : targetIds) {
            LinkedList<CurrencyPlayerData.PlayerCurrency> list = CurrencyPlayerData.SERVER.getPlayersCurrency(playerId);
            CurrencyInspectResult inspect = inspectEntries(list);
            if (inspect.missing() == 0 && inspect.dangling() == 0) {
                continue;
            }
            CurrencyRepairResult result = repairEntries(playerId, list);
            repairedPlayers++;
            added += result.added();
            removed += result.removed();
            effects.touchPlayer(playerId);
        }
        effects.dispatch();
        return new CurrencyBatchRepairResult(repairedPlayers, added, removed);
    }

    public static CurrencyBatchRepairResult repairAll(MinecraftServer server) {
        int players = 0;
        int added = 0;
        int removed = 0;
        EconomyOperationEffects effects = new EconomyOperationEffects();
        if (server != null) {
            effects.useServer(server).requestSave();
        }
        for (Map.Entry<UUID, LinkedList<CurrencyPlayerData.PlayerCurrency>> entry : CurrencyPlayerData.SERVER.playersCurrencyMap.entrySet()) {
            players++;
            CurrencyRepairResult result = repairEntries(entry.getKey(), entry.getValue());
            added += result.added();
            removed += result.removed();
            effects.touchPlayer(entry.getKey());
        }
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                CurrencyPlayerData.SERVER.newPlayer(player);
                effects.touchPlayer(player);
            }
        }
        effects.dispatch();
        return new CurrencyBatchRepairResult(players, added, removed);
    }

    private static Set<String> registryCurrencyIds() {
        return CurrencyData.SERVER.currencies.stream()
            .map(BaseCurrency::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static CurrencyInspectResult inspectEntries(LinkedList<CurrencyPlayerData.PlayerCurrency> currencies) {
        Set<String> registryCurrencies = registryCurrencyIds();
        Set<String> playerCurrencyIds = currencies.stream()
            .map(pc -> pc.currency.getName())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        int lockedCount = 0;
        int missingCount = 0;
        int danglingCount = 0;
        for (CurrencyPlayerData.PlayerCurrency currency : currencies) {
            if (currency.isLocked) {
                lockedCount++;
            }
            if (!registryCurrencies.contains(currency.currency.getName())) {
                danglingCount++;
            }
        }
        for (String currencyId : registryCurrencies) {
            if (!playerCurrencyIds.contains(currencyId)) {
                missingCount++;
            }
        }
        return new CurrencyInspectResult(registryCurrencies.size(), currencies.size(), lockedCount, missingCount, danglingCount);
    }

    private static CurrencyRepairResult repairEntries(UUID playerId, LinkedList<CurrencyPlayerData.PlayerCurrency> list) {
        Map<String, BaseCurrency> registry = CurrencyData.SERVER.currencies.stream()
            .collect(Collectors.toMap(
                BaseCurrency::getName,
                BaseCurrency::copy,
                (a, b) -> a,
                LinkedHashMap::new
            ));
        int added = 0;
        int removed = 0;
        Set<String> seen = new HashSet<>();
        Iterator<CurrencyPlayerData.PlayerCurrency> iterator = list.iterator();
        while (iterator.hasNext()) {
            CurrencyPlayerData.PlayerCurrency playerCurrency = iterator.next();
            String currencyId = playerCurrency.currency.getName();
            if (!registry.containsKey(currencyId) || !seen.add(currencyId)) {
                iterator.remove();
                removed++;
            }
        }
        for (Map.Entry<String, BaseCurrency> currencyEntry : registry.entrySet()) {
            if (!seen.contains(currencyEntry.getKey())) {
                list.add(new CurrencyPlayerData.PlayerCurrency(currencyEntry.getValue().copy(), currencyEntry.getValue().getDefaultValue()));
                added++;
            }
        }
        return new CurrencyRepairResult(playerId, added, removed);
    }

    public record CurrencyAuditResult(int registered, int players, int missingEntries, int danglingEntries) {
    }

    public record CurrencyInspectResult(int registered, int entries, int locked, int missing, int dangling) {
    }

    public record CurrencyRepairResult(UUID playerId, int added, int removed) {
    }

    public record CurrencyPlayerState(UUID playerId, boolean present, double balance, boolean locked) {
    }

    public record CurrencyPlayersReport(int playerCount, int present, int missing, int locked, LinkedList<CurrencyPlayerState> states) {
    }

    public record PlayerIssue(UUID playerId, int entries, int missing, int dangling, int locked) {
    }

    public record CurrencyIssuesReport(int checked, LinkedList<PlayerIssue> issues) {
    }

    public record CurrencySummaryLine(String currencyId, int present, int missing, int locked) {
    }

    public record CurrencyBatchRepairResult(int players, int added, int removed) {
    }
}
