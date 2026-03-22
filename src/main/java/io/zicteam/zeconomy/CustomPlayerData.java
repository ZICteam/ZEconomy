package io.zicteam.zeconomy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public class CustomPlayerData {
    public static CustomPlayerData.Server SERVER = new CustomPlayerData.Server();
    public static CustomPlayerData.Client CLIENT = new CustomPlayerData.Client(new Data(new CompoundTag()));

    public CustomPlayerData() {
    }

    public static class Data {
        public CompoundTag nbt;

        public Data(CompoundTag nbt) {
            this.nbt = nbt == null ? new CompoundTag() : nbt;
        }
    }

    public static class Server {
        public Map<UUID, Data> playersData;

        public Server() {
            this.playersData = new ConcurrentHashMap<>();
        }

        public Data getPlayerCustomData(Player player) {
            return getPlayerCustomData(player.getUUID());
        }

        public Data getPlayerCustomData(UUID id) {
            createData(id);
            return playersData.get(id);
        }

        public void createData(Player player) {
            createData(player.getUUID());
        }

        public void createData(UUID id) {
            playersData.computeIfAbsent(id, ignored -> new Data(new CompoundTag()));
        }

        public CompoundTag serialize() {
            CompoundTag root = new CompoundTag();
            ListTag players = new ListTag();
            for (Map.Entry<UUID, Data> entry : playersData.entrySet()) {
                CompoundTag player = new CompoundTag();
                player.putUUID("uuid", entry.getKey());
                player.put("data", entry.getValue().nbt.copy());
                players.add(player);
            }
            root.put("players", players);
            return root;
        }

        public static Server deserialize(CompoundTag root) {
            Server server = new Server();
            if (root == null) {
                return server;
            }
            ListTag players = root.getList("players", Tag.TAG_COMPOUND);
            for (Tag tag : players) {
                if (!(tag instanceof CompoundTag playerTag)) {
                    continue;
                }
                UUID id = playerTag.getUUID("uuid");
                CompoundTag data = playerTag.contains("data", Tag.TAG_COMPOUND) ? playerTag.getCompound("data") : new CompoundTag();
                server.playersData.put(id, new Data(data));
            }
            return server;
        }

        public void save(Path path) {
            try {
                Files.createDirectories(path.getParent());
                NbtIo.writeCompressed(serialize(), path.toFile());
            } catch (IOException e) {
                ZEconomy.printStackTrace("Failed to save custom player data", e);
            }
        }

        public static Server load(Path path) {
            if (path == null || !Files.exists(path)) {
                return new Server();
            }
            try {
                CompoundTag root = NbtIo.readCompressed(path.toFile());
                return deserialize(root);
            } catch (IOException e) {
                ZEconomy.printStackTrace("Failed to load custom player data", e);
                return new Server();
            }
        }
    }

    public static class Client {
        public Data data;

        public Client(Data data) {
            this.data = data == null ? new Data(new CompoundTag()) : data;
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.put("data", data.nbt.copy());
            return tag;
        }

        public static Client deserialize(CompoundTag root) {
            if (root == null || !root.contains("data", Tag.TAG_COMPOUND)) {
                return new Client(new Data(new CompoundTag()));
            }
            return new Client(new Data(root.getCompound("data")));
        }
    }
}
