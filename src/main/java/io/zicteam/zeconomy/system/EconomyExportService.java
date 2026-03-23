package io.zicteam.zeconomy.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.utils.CurrencyHelper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import net.minecraft.server.MinecraftServer;

public final class EconomyExportService {
    private EconomyExportService() {
    }

    public static void tickExport(MinecraftServer server) {
        long now = Instant.now().getEpochSecond();
        if (!ZEconomy.EXTRA_DATA.shouldExport(now, EconomyConfig.EXPORT_INTERVAL_SECONDS.get())) {
            return;
        }
        exportJson(server, CurrencyHelper.exportJsonPath(server));
        ZEconomy.EXTRA_DATA.markExported(now);
    }

    public static Path exportNow(MinecraftServer server) {
        Path exportPath = CurrencyHelper.exportJsonPath(server);
        exportJson(server, exportPath);
        return exportPath;
    }

    public static void exportJson(MinecraftServer server, Path exportPath) {
        try {
            Files.createDirectories(exportPath.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject root = new JsonObject();
            root.addProperty("generated_at_epoch", Instant.now().getEpochSecond());
            root.addProperty("server_players_online", server.getPlayerList().getPlayerCount());

            JsonArray tops = new JsonArray();
            for (ExtraEconomyData.RichEntry entry : EconomyReadService.getTopRich(server, ZEconomy.PRIMARY_CURRENCY_ID, 20, true)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", entry.playerId().toString());
                obj.addProperty("name", entry.playerName());
                obj.addProperty("currency", entry.currency());
                obj.addProperty("amount", entry.amount());
                tops.add(obj);
            }
            root.add("top_" + ZEconomy.PRIMARY_CURRENCY_ID, tops);

            JsonArray logsJson = new JsonArray();
            for (ExtraEconomyData.TransactionRecord record : EconomySnapshotReadService.recentLogs(100)) {
                JsonObject obj = new JsonObject();
                obj.addProperty("time", record.epochSec());
                obj.addProperty("type", record.type());
                obj.addProperty("actor", record.actor() == null ? "" : record.actor().toString());
                obj.addProperty("target", record.target() == null ? "" : record.target().toString());
                obj.addProperty("currency", record.currency());
                obj.addProperty("amount", record.amount());
                obj.addProperty("fee", record.fee());
                obj.addProperty("note", record.note());
                logsJson.add(obj);
            }
            root.add("recent_logs", logsJson);

            Files.writeString(exportPath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to export economy JSON", e);
        }
    }
}
