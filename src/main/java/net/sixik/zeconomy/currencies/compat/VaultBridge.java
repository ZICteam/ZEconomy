package net.sixik.zeconomy.currencies.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.sixik.zeconomy.ZEconomy;

public final class VaultBridge {
    private static boolean available = false;
    private static Object economyProvider;
    private static Method bukkitGetOfflinePlayer;
    private static Method economyGetBalance;
    private static Method economyDeposit;
    private static Method economyWithdraw;

    private VaultBridge() {
    }

    public static void init(MinecraftServer server) {
        if (available) {
            return;
        }
        try {
            Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
            Class<?> offlinePlayerClass = Class.forName("org.bukkit.OfflinePlayer");
            Class<?> servicesManagerClass = Class.forName("org.bukkit.plugin.ServicesManager");
            Class<?> registeredProviderClass = Class.forName("org.bukkit.plugin.RegisteredServiceProvider");
            Class<?> economyClass = loadVaultEconomyClass(bukkitClass);
            if (economyClass == null) {
                available = false;
                ZEconomy.LOGGER.info("[ZEconomy] Vault bridge not available: Vault Economy class missing");
                return;
            }

            Method getServicesManager = bukkitClass.getMethod("getServicesManager");
            Method getRegistration = servicesManagerClass.getMethod("getRegistration", Class.class);
            Method getProvider = registeredProviderClass.getMethod("getProvider");
            bukkitGetOfflinePlayer = bukkitClass.getMethod("getOfflinePlayer", UUID.class);

            Object servicesManager = getServicesManager.invoke(null);
            if (servicesManager == null) {
                return;
            }
            Object registration = getRegistration.invoke(servicesManager, economyClass);
            if (registration == null) {
                return;
            }
            economyProvider = getProvider.invoke(registration);
            if (economyProvider == null) {
                return;
            }

            economyGetBalance = economyClass.getMethod("getBalance", offlinePlayerClass);
            economyDeposit = economyClass.getMethod("depositPlayer", offlinePlayerClass, double.class);
            economyWithdraw = economyClass.getMethod("withdrawPlayer", offlinePlayerClass, double.class);
            available = true;
            ZEconomy.LOGGER.info("[ZEconomy] Vault bridge enabled");
        } catch (Throwable t) {
            available = false;
            ZEconomy.LOGGER.info("[ZEconomy] Vault bridge not available: {}", t.getClass().getSimpleName());
        }
    }

    private static Class<?> loadVaultEconomyClass(Class<?> bukkitClass) {
        try {
            return Class.forName("net.milkbowl.vault.economy.Economy");
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class<?> pluginClass = Class.forName("org.bukkit.plugin.Plugin");
            Class<?> pluginManagerClass = Class.forName("org.bukkit.plugin.PluginManager");
            Method getPluginManager = bukkitClass.getMethod("getPluginManager");
            Method getPlugin = pluginManagerClass.getMethod("getPlugin", String.class);
            Object pluginManager = getPluginManager.invoke(null);
            if (pluginManager == null) {
                return null;
            }
            Object vaultPlugin = getPlugin.invoke(pluginManager, "Vault");
            if (vaultPlugin == null || !pluginClass.isInstance(vaultPlugin)) {
                return null;
            }
            ClassLoader loader = vaultPlugin.getClass().getClassLoader();
            if (loader == null) {
                return null;
            }
            return Class.forName("net.milkbowl.vault.economy.Economy", true, loader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static String getProviderName() {
        if (!available || economyProvider == null) {
            return "unavailable";
        }
        return economyProvider.getClass().getName();
    }

    public static double readBalance(UUID playerId) {
        if (!available || playerId == null) {
            return Double.NaN;
        }
        try {
            Object offlinePlayer = bukkitGetOfflinePlayer.invoke(null, playerId);
            Object value = economyGetBalance.invoke(economyProvider, offlinePlayer);
            return value instanceof Number number ? number.doubleValue() : Double.NaN;
        } catch (Throwable t) {
            ZEconomy.LOGGER.warn("[ZEconomy] Vault read failed for {}: {}", playerId, t.getClass().getSimpleName());
            return Double.NaN;
        }
    }

    public static boolean writeBalance(UUID playerId, double targetBalance) {
        if (!available || playerId == null || targetBalance < 0.0) {
            return false;
        }
        try {
            Object offlinePlayer = bukkitGetOfflinePlayer.invoke(null, playerId);
            Object currentObj = economyGetBalance.invoke(economyProvider, offlinePlayer);
            double current = currentObj instanceof Number number ? number.doubleValue() : 0.0;
            double delta = targetBalance - current;
            if (Math.abs(delta) < 0.000001D) {
                return true;
            }
            if (delta > 0.0) {
                economyDeposit.invoke(economyProvider, offlinePlayer, delta);
            } else {
                economyWithdraw.invoke(economyProvider, offlinePlayer, -delta);
            }
            return true;
        } catch (Throwable t) {
            ZEconomy.LOGGER.warn("[ZEconomy] Vault write failed for {}: {}", playerId, t.getClass().getSimpleName());
            return false;
        }
    }
}
