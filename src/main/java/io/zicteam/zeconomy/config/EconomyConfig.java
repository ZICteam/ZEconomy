package io.zicteam.zeconomy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class EconomyConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_PHYSICAL_MONEY;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MAILBOX;
    public static final ForgeConfigSpec.BooleanValue ENABLE_BCOIN_EXCHANGE;
    public static final ForgeConfigSpec.DoubleValue HOURLY_INTEREST_RATE;
    public static final ForgeConfigSpec.DoubleValue DEFAULT_BCOIN_TO_Z_RATE;
    public static final ForgeConfigSpec.DoubleValue ATM_WITHDRAW_STEP;
    public static final ForgeConfigSpec.DoubleValue TRANSFER_FEE_RATE;
    public static final ForgeConfigSpec.DoubleValue EXCHANGE_FEE_RATE;
    public static final ForgeConfigSpec.DoubleValue DAILY_REWARD_Z;
    public static final ForgeConfigSpec.DoubleValue DAILY_REWARD_B;
    public static final ForgeConfigSpec.IntValue EXPORT_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.BooleanValue USE_PERMISSION_NODES;
    public static final ForgeConfigSpec.BooleanValue ALLOW_OP_FALLBACK;
    public static final ForgeConfigSpec.IntValue OP_LEVEL_ADMIN;
    public static final ForgeConfigSpec.IntValue OP_LEVEL_MODERATOR;
    public static final ForgeConfigSpec.BooleanValue ENABLE_VAULT_BRIDGE;
    public static final ForgeConfigSpec.BooleanValue VAULT_PULL_ON_JOIN;
    public static final ForgeConfigSpec.BooleanValue VAULT_PUSH_ON_CHANGE;
    public static final ForgeConfigSpec.ConfigValue<String> VAULT_SYNC_CURRENCY_ID;
    public static final ForgeConfigSpec.ConfigValue<String> STORAGE_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> JSON_FILE_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> SQLITE_FILE_NAME;
    public static final ForgeConfigSpec.ConfigValue<String> MYSQL_HOST;
    public static final ForgeConfigSpec.IntValue MYSQL_PORT;
    public static final ForgeConfigSpec.ConfigValue<String> MYSQL_DATABASE;
    public static final ForgeConfigSpec.ConfigValue<String> MYSQL_USER;
    public static final ForgeConfigSpec.ConfigValue<String> MYSQL_PASSWORD;
    public static final ForgeConfigSpec.ConfigValue<String> MYSQL_TABLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        ENABLE_PHYSICAL_MONEY = builder
            .comment("Enable physical money items. If false, only digital currency is used.")
            .define("enablePhysicalMoney", true);
        ENABLE_MAILBOX = builder
            .comment("Enable mailbox system for item transfer between players.")
            .define("enableMailbox", true);
        ENABLE_BCOIN_EXCHANGE = builder
            .comment("Enable exchange from b_coin to z_coin and other configured rates.")
            .define("enableBcoinExchange", true);
        HOURLY_INTEREST_RATE = builder
            .comment("Hourly interest payout rate for bank deposits. Example: 0.01 = 1% per hour.")
            .defineInRange("hourlyInterestRate", 0.01D, 0.0D, 1.0D);
        DEFAULT_BCOIN_TO_Z_RATE = builder
            .comment("Default exchange rate b_coin -> z_coin.")
            .defineInRange("defaultBcoinToZRate", 100.0D, 0.0001D, 1000000.0D);
        ATM_WITHDRAW_STEP = builder
            .comment("ATM right-click sneaking withdraw amount (physical items).")
            .defineInRange("atmWithdrawStep", 100.0D, 1.0D, 1000000.0D);
        TRANSFER_FEE_RATE = builder
            .comment("Fee rate for player transfer/pay actions. Example: 0.02 = 2%.")
            .defineInRange("transferFeeRate", 0.01D, 0.0D, 1.0D);
        EXCHANGE_FEE_RATE = builder
            .comment("Fee rate for currency exchange actions. Example: 0.03 = 3%.")
            .defineInRange("exchangeFeeRate", 0.02D, 0.0D, 1.0D);
        DAILY_REWARD_Z = builder
            .comment("Daily reward amount for z_coin.")
            .defineInRange("dailyRewardZ", 250.0D, 0.0D, 10000000.0D);
        DAILY_REWARD_B = builder
            .comment("Daily reward amount for b_coin.")
            .defineInRange("dailyRewardB", 1.0D, 0.0D, 10000000.0D);
        EXPORT_INTERVAL_SECONDS = builder
            .comment("Economy JSON export interval in seconds.")
            .defineInRange("exportIntervalSeconds", 60, 10, 86400);
        builder.pop();

        builder.push("permissions");
        USE_PERMISSION_NODES = builder
            .comment("Enable Forge PermissionAPI nodes (recommended for LuckPerms bridges).")
            .define("usePermissionNodes", true);
        ALLOW_OP_FALLBACK = builder
            .comment("Allow vanilla OP level fallback when permission node is missing.")
            .define("allowOpFallback", true);
        OP_LEVEL_ADMIN = builder
            .comment("Fallback OP level for admin commands.")
            .defineInRange("opLevelAdmin", 2, 0, 4);
        OP_LEVEL_MODERATOR = builder
            .comment("Fallback OP level for moderator/user elevated commands.")
            .defineInRange("opLevelModerator", 1, 0, 4);
        builder.pop();

        builder.push("compat");
        builder.push("vault");
        ENABLE_VAULT_BRIDGE = builder
            .comment("Enable runtime bridge to Bukkit Vault economy on hybrid servers (Mohist/Arclight/CatServer).")
            .define("enableVaultBridge", true);
        VAULT_PULL_ON_JOIN = builder
            .comment("On player join, pull Vault balance into selected ZEconomy currency.")
            .define("pullVaultBalanceOnJoin", true);
        VAULT_PUSH_ON_CHANGE = builder
            .comment("Push ZEconomy balance changes to Vault provider for selected currency.")
            .define("pushToVaultOnBalanceChange", true);
        VAULT_SYNC_CURRENCY_ID = builder
            .comment("Currency id that is synchronized with Vault.")
            .define("syncCurrencyId", "z_coin");
        builder.pop();

        builder.push("storage");
        STORAGE_MODE = builder
            .comment("Data storage mode: nbt | json | sqlite | mysql")
            .define("mode", "nbt");
        JSON_FILE_NAME = builder
            .comment("JSON storage file name (used when mode=json).")
            .define("jsonFileName", "economy_data.json");
        SQLITE_FILE_NAME = builder
            .comment("SQLite file name (used when mode=sqlite).")
            .define("sqliteFileName", "economy_data.db");
        MYSQL_HOST = builder
            .comment("MySQL host (used when mode=mysql).")
            .define("mysqlHost", "127.0.0.1");
        MYSQL_PORT = builder
            .comment("MySQL port (used when mode=mysql).")
            .defineInRange("mysqlPort", 3306, 1, 65535);
        MYSQL_DATABASE = builder
            .comment("MySQL database name.")
            .define("mysqlDatabase", "zeconomy");
        MYSQL_USER = builder
            .comment("MySQL username.")
            .define("mysqlUser", "root");
        MYSQL_PASSWORD = builder
            .comment("MySQL password.")
            .define("mysqlPassword", "change_me");
        MYSQL_TABLE = builder
            .comment("MySQL/SQLite table for snapshot storage.")
            .define("sqlTable", "zeconomy_storage");
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private EconomyConfig() {
    }
}
