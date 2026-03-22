package io.zicteam.zeconomy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.nbt.CompoundTag;
import io.zicteam.zeconomy.ZEconomy;

public final class GuiLayoutConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("zeconomy");
    private static final Path ATM_FILE = ROOT.resolve("gui_atm.json");
    private static final Path BANK_FILE = ROOT.resolve("gui_bank.json");
    private static final Path MAILBOX_FILE = ROOT.resolve("gui_mailbox.json");
    private static final Path EXCHANGE_FILE = ROOT.resolve("gui_exchange.json");
    private static final Path THEME_FILE = ROOT.resolve("gui_theme.json");
    private static final Path README_FILE = ROOT.resolve("README.txt");

    private static AtmLayout atm = AtmLayout.defaults();
    private static BankLayout bank = BankLayout.defaults();
    private static MailboxLayout mailbox = MailboxLayout.defaults();
    private static ExchangeLayout exchange = ExchangeLayout.defaults();
    private static GuiTheme theme = GuiTheme.defaults();

    private GuiLayoutConfig() {
    }

    public static void reload() {
        try {
            Files.createDirectories(ROOT);
            writeReadmeIfMissing();
            atm = loadOrCreate(ATM_FILE, AtmLayout.class, AtmLayout.defaults(), atm);
            atm.migrateLegacyIfNeeded();
            bank = loadOrCreate(BANK_FILE, BankLayout.class, BankLayout.defaults(), bank);
            mailbox = loadOrCreate(MAILBOX_FILE, MailboxLayout.class, MailboxLayout.defaults(), mailbox);
            exchange = loadOrCreate(EXCHANGE_FILE, ExchangeLayout.class, ExchangeLayout.defaults(), exchange);
            theme = loadOrCreate(THEME_FILE, GuiTheme.class, GuiTheme.defaults(), theme);
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to reload GUI layouts", e);
        }
    }

    public static AtmLayout atm() {
        return atm;
    }

    public static MailboxLayout mailbox() {
        return mailbox;
    }

    public static BankLayout bank() {
        return bank;
    }

    public static ExchangeLayout exchange() {
        return exchange;
    }

    public static GuiTheme theme() {
        return theme;
    }

    public static boolean isValidTarget(String target) {
        return "exchange".equalsIgnoreCase(target) || "atm".equalsIgnoreCase(target) || "mailbox".equalsIgnoreCase(target) || "bank".equalsIgnoreCase(target);
    }

    public static CompoundTag toTag(String target) {
        CompoundTag tag = new CompoundTag();
        if ("exchange".equalsIgnoreCase(target)) {
            ExchangeLayout e = exchange();
            put(tag, "imageWidth", e.imageWidth);
            put(tag, "imageHeight", e.imageHeight);
            put(tag, "templateSlotX", e.templateSlotX);
            put(tag, "inputSlotY", e.inputSlotY);
            put(tag, "outputSlotY", e.outputSlotY);
            put(tag, "textX", e.textX);
            put(tag, "acceptsLabelY", e.acceptsLabelY);
            put(tag, "acceptsValueY", e.acceptsValueY);
            put(tag, "givesLabelY", e.givesLabelY);
            put(tag, "givesValueY", e.givesValueY);
            put(tag, "storageX", e.storageX);
            put(tag, "storageLabelY", e.storageLabelY);
            put(tag, "storageY", e.storageY);
            put(tag, "playerInvX", e.playerInvX);
            put(tag, "playerInvY", e.playerInvY);
            put(tag, "hotbarX", e.hotbarX);
            put(tag, "hotbarY", e.hotbarY);
            put(tag, "tradeButtonX", e.tradeButtonX);
            put(tag, "tradeButtonY", e.tradeButtonY);
            put(tag, "tradeButtonW", e.tradeButtonW);
            put(tag, "tradeButtonH", e.tradeButtonH);
            put(tag, "maxTextWidth", e.maxTextWidth);
            tag.putBoolean("table.enabled", e.table.enabled);
            put(tag, "table.originX", e.table.originX);
            put(tag, "table.originY", e.table.originY);
            put(tag, "table.colStep", e.table.colStep);
            put(tag, "table.rowStep", e.table.rowStep);
            put(tag, "table.templateSlotCol", e.table.templateSlotCol);
            put(tag, "table.inputSlotRow", e.table.inputSlotRow);
            put(tag, "table.outputSlotRow", e.table.outputSlotRow);
            put(tag, "table.textCol", e.table.textCol);
            put(tag, "table.acceptsLabelRow", e.table.acceptsLabelRow);
            put(tag, "table.acceptsValueRow", e.table.acceptsValueRow);
            put(tag, "table.givesLabelRow", e.table.givesLabelRow);
            put(tag, "table.givesValueRow", e.table.givesValueRow);
            put(tag, "table.storageCol", e.table.storageCol);
            put(tag, "table.storageLabelRow", e.table.storageLabelRow);
            put(tag, "table.storageRow", e.table.storageRow);
            put(tag, "table.tradeButtonRow", e.table.tradeButtonRow);
            return tag;
        }
        if ("atm".equalsIgnoreCase(target)) {
            AtmLayout a = atm();
            put(tag, "imageWidth", a.imageWidth);
            put(tag, "imageHeight", a.imageHeight);
            put(tag, "amountX", a.amountX);
            put(tag, "amountY", a.amountY);
            put(tag, "amountW", a.amountW);
            put(tag, "amountH", a.amountH);
            put(tag, "depositZX", a.depositZX);
            put(tag, "depositZY", a.depositZY);
            put(tag, "withdrawZX", a.withdrawZX);
            put(tag, "withdrawZY", a.withdrawZY);
            put(tag, "depositBX", a.depositBX);
            put(tag, "depositBY", a.depositBY);
            put(tag, "withdrawBX", a.withdrawBX);
            put(tag, "withdrawBY", a.withdrawBY);
            put(tag, "bankX", a.bankX);
            put(tag, "bankY", a.bankY);
            put(tag, "depositAllZX", a.depositAllZX);
            put(tag, "depositAllZY", a.depositAllZY);
            put(tag, "depositAllBX", a.depositAllBX);
            put(tag, "depositAllBY", a.depositAllBY);
            put(tag, "refreshX", a.refreshX);
            put(tag, "refreshY", a.refreshY);
            put(tag, "closeX", a.closeX);
            put(tag, "closeY", a.closeY);
            put(tag, "leftButtonX", a.leftButtonX);
            put(tag, "rightButtonX", a.rightButtonX);
            put(tag, "row1Y", a.row1Y);
            put(tag, "row2Y", a.row2Y);
            put(tag, "row3Y", a.row3Y);
            put(tag, "bottomY", a.bottomY);
            put(tag, "buttonW", a.buttonW);
            put(tag, "buttonH", a.buttonH);
            put(tag, "smallButtonW", a.smallButtonW);
            put(tag, "closeButtonX", a.closeButtonX);
            put(tag, "walletY", a.walletY);
            put(tag, "invY", a.invY);
            return tag;
        }
        if ("bank".equalsIgnoreCase(target)) {
            BankLayout b = bank();
            put(tag, "imageWidth", b.imageWidth);
            put(tag, "imageHeight", b.imageHeight);
            put(tag, "amountX", b.amountX);
            put(tag, "amountY", b.amountY);
            put(tag, "amountW", b.amountW);
            put(tag, "amountH", b.amountH);
            put(tag, "currencyZX", b.currencyZX);
            put(tag, "currencyZY", b.currencyZY);
            put(tag, "currencyZW", b.currencyZW);
            put(tag, "currencyBX", b.currencyBX);
            put(tag, "currencyBY", b.currencyBY);
            put(tag, "currencyBW", b.currencyBW);
            put(tag, "depositX", b.depositX);
            put(tag, "depositY", b.depositY);
            put(tag, "withdrawX", b.withdrawX);
            put(tag, "withdrawY", b.withdrawY);
            put(tag, "exchangeX", b.exchangeX);
            put(tag, "exchangeY", b.exchangeY);
            put(tag, "claimMailX", b.claimMailX);
            put(tag, "claimMailY", b.claimMailY);
            put(tag, "refreshX", b.refreshX);
            put(tag, "refreshY", b.refreshY);
            put(tag, "closeX", b.closeX);
            put(tag, "closeY", b.closeY);
            put(tag, "headerX", b.headerX);
            put(tag, "headerY", b.headerY);
            put(tag, "amountLabelX", b.amountLabelX);
            put(tag, "amountLabelY", b.amountLabelY);
            put(tag, "selectedX", b.selectedX);
            put(tag, "selectedY", b.selectedY);
            put(tag, "walletZX", b.walletZX);
            put(tag, "walletZY", b.walletZY);
            put(tag, "walletBX", b.walletBX);
            put(tag, "walletBY", b.walletBY);
            put(tag, "bankZX", b.bankZX);
            put(tag, "bankZY", b.bankZY);
            put(tag, "bankBX", b.bankBX);
            put(tag, "bankBY", b.bankBY);
            put(tag, "mailX", b.mailX);
            put(tag, "mailY", b.mailY);
            return tag;
        }
        MailboxLayout m = mailbox();
        put(tag, "imageWidth", m.imageWidth);
        put(tag, "imageHeight", m.imageHeight);
        put(tag, "recipientX", m.recipientX);
        put(tag, "recipientY", m.recipientY);
        put(tag, "recipientW", m.recipientW);
        put(tag, "recipientH", m.recipientH);
        put(tag, "sendButtonX", m.sendButtonX);
        put(tag, "sendButtonY", m.sendButtonY);
        put(tag, "claimButtonX", m.claimButtonX);
        put(tag, "claimButtonY", m.claimButtonY);
        put(tag, "buttonW", m.buttonW);
        put(tag, "buttonH", m.buttonH);
        return tag;
    }

    public static boolean applyTagAndSave(String target, CompoundTag tag) {
        try {
            if ("exchange".equalsIgnoreCase(target)) {
                ExchangeLayout e = exchange();
                applyInt(tag, "imageWidth", v -> e.imageWidth = v);
                applyInt(tag, "imageHeight", v -> e.imageHeight = v);
                applyInt(tag, "templateSlotX", v -> e.templateSlotX = v);
                applyInt(tag, "inputSlotY", v -> e.inputSlotY = v);
                applyInt(tag, "outputSlotY", v -> e.outputSlotY = v);
                applyInt(tag, "textX", v -> e.textX = v);
                applyInt(tag, "acceptsLabelY", v -> e.acceptsLabelY = v);
                applyInt(tag, "acceptsValueY", v -> e.acceptsValueY = v);
                applyInt(tag, "givesLabelY", v -> e.givesLabelY = v);
                applyInt(tag, "givesValueY", v -> e.givesValueY = v);
                applyInt(tag, "storageX", v -> e.storageX = v);
                applyInt(tag, "storageLabelY", v -> e.storageLabelY = v);
                applyInt(tag, "storageY", v -> e.storageY = v);
                applyInt(tag, "playerInvX", v -> e.playerInvX = v);
                applyInt(tag, "playerInvY", v -> e.playerInvY = v);
                applyInt(tag, "hotbarX", v -> e.hotbarX = v);
                applyInt(tag, "hotbarY", v -> e.hotbarY = v);
                applyInt(tag, "tradeButtonX", v -> e.tradeButtonX = v);
                applyInt(tag, "tradeButtonY", v -> e.tradeButtonY = v);
                applyInt(tag, "tradeButtonW", v -> e.tradeButtonW = v);
                applyInt(tag, "tradeButtonH", v -> e.tradeButtonH = v);
                applyInt(tag, "maxTextWidth", v -> e.maxTextWidth = v);
                if (tag.contains("table.enabled")) {
                    e.table.enabled = tag.getBoolean("table.enabled");
                }
                applyInt(tag, "table.originX", v -> e.table.originX = v);
                applyInt(tag, "table.originY", v -> e.table.originY = v);
                applyInt(tag, "table.colStep", v -> e.table.colStep = v);
                applyInt(tag, "table.rowStep", v -> e.table.rowStep = v);
                applyInt(tag, "table.templateSlotCol", v -> e.table.templateSlotCol = v);
                applyInt(tag, "table.inputSlotRow", v -> e.table.inputSlotRow = v);
                applyInt(tag, "table.outputSlotRow", v -> e.table.outputSlotRow = v);
                applyInt(tag, "table.textCol", v -> e.table.textCol = v);
                applyInt(tag, "table.acceptsLabelRow", v -> e.table.acceptsLabelRow = v);
                applyInt(tag, "table.acceptsValueRow", v -> e.table.acceptsValueRow = v);
                applyInt(tag, "table.givesLabelRow", v -> e.table.givesLabelRow = v);
                applyInt(tag, "table.givesValueRow", v -> e.table.givesValueRow = v);
                applyInt(tag, "table.storageCol", v -> e.table.storageCol = v);
                applyInt(tag, "table.storageLabelRow", v -> e.table.storageLabelRow = v);
                applyInt(tag, "table.storageRow", v -> e.table.storageRow = v);
                applyInt(tag, "table.tradeButtonRow", v -> e.table.tradeButtonRow = v);
                Files.createDirectories(ROOT);
                Files.writeString(EXCHANGE_FILE, GSON.toJson(e), StandardCharsets.UTF_8);
                reload();
                return true;
            }
            if ("atm".equalsIgnoreCase(target)) {
                AtmLayout a = atm();
                applyInt(tag, "imageWidth", v -> a.imageWidth = v);
                applyInt(tag, "imageHeight", v -> a.imageHeight = v);
                applyInt(tag, "amountX", v -> a.amountX = v);
                applyInt(tag, "amountY", v -> a.amountY = v);
                applyInt(tag, "amountW", v -> a.amountW = v);
                applyInt(tag, "amountH", v -> a.amountH = v);
                applyInt(tag, "depositZX", v -> a.depositZX = v);
                applyInt(tag, "depositZY", v -> a.depositZY = v);
                applyInt(tag, "withdrawZX", v -> a.withdrawZX = v);
                applyInt(tag, "withdrawZY", v -> a.withdrawZY = v);
                applyInt(tag, "depositBX", v -> a.depositBX = v);
                applyInt(tag, "depositBY", v -> a.depositBY = v);
                applyInt(tag, "withdrawBX", v -> a.withdrawBX = v);
                applyInt(tag, "withdrawBY", v -> a.withdrawBY = v);
                applyInt(tag, "bankX", v -> a.bankX = v);
                applyInt(tag, "bankY", v -> a.bankY = v);
                applyInt(tag, "depositAllZX", v -> a.depositAllZX = v);
                applyInt(tag, "depositAllZY", v -> a.depositAllZY = v);
                applyInt(tag, "depositAllBX", v -> a.depositAllBX = v);
                applyInt(tag, "depositAllBY", v -> a.depositAllBY = v);
                applyInt(tag, "refreshX", v -> a.refreshX = v);
                applyInt(tag, "refreshY", v -> a.refreshY = v);
                applyInt(tag, "closeX", v -> a.closeX = v);
                applyInt(tag, "closeY", v -> a.closeY = v);
                applyInt(tag, "leftButtonX", v -> a.leftButtonX = v);
                applyInt(tag, "rightButtonX", v -> a.rightButtonX = v);
                applyInt(tag, "row1Y", v -> a.row1Y = v);
                applyInt(tag, "row2Y", v -> a.row2Y = v);
                applyInt(tag, "row3Y", v -> a.row3Y = v);
                applyInt(tag, "bottomY", v -> a.bottomY = v);
                applyInt(tag, "buttonW", v -> a.buttonW = v);
                applyInt(tag, "buttonH", v -> a.buttonH = v);
                applyInt(tag, "smallButtonW", v -> a.smallButtonW = v);
                applyInt(tag, "closeButtonX", v -> a.closeButtonX = v);
                applyInt(tag, "walletY", v -> a.walletY = v);
                applyInt(tag, "invY", v -> a.invY = v);
                Files.createDirectories(ROOT);
                Files.writeString(ATM_FILE, GSON.toJson(a), StandardCharsets.UTF_8);
                reload();
                return true;
            }
            if ("mailbox".equalsIgnoreCase(target)) {
                MailboxLayout m = mailbox();
                applyInt(tag, "imageWidth", v -> m.imageWidth = v);
                applyInt(tag, "imageHeight", v -> m.imageHeight = v);
                applyInt(tag, "recipientX", v -> m.recipientX = v);
                applyInt(tag, "recipientY", v -> m.recipientY = v);
                applyInt(tag, "recipientW", v -> m.recipientW = v);
                applyInt(tag, "recipientH", v -> m.recipientH = v);
                applyInt(tag, "sendButtonX", v -> m.sendButtonX = v);
                applyInt(tag, "sendButtonY", v -> m.sendButtonY = v);
                applyInt(tag, "claimButtonX", v -> m.claimButtonX = v);
                applyInt(tag, "claimButtonY", v -> m.claimButtonY = v);
                applyInt(tag, "buttonW", v -> m.buttonW = v);
                applyInt(tag, "buttonH", v -> m.buttonH = v);
                Files.createDirectories(ROOT);
                Files.writeString(MAILBOX_FILE, GSON.toJson(m), StandardCharsets.UTF_8);
                reload();
                return true;
            }
            if ("bank".equalsIgnoreCase(target)) {
                BankLayout b = bank();
                applyInt(tag, "imageWidth", v -> b.imageWidth = v);
                applyInt(tag, "imageHeight", v -> b.imageHeight = v);
                applyInt(tag, "amountX", v -> b.amountX = v);
                applyInt(tag, "amountY", v -> b.amountY = v);
                applyInt(tag, "amountW", v -> b.amountW = v);
                applyInt(tag, "amountH", v -> b.amountH = v);
                applyInt(tag, "currencyZX", v -> b.currencyZX = v);
                applyInt(tag, "currencyZY", v -> b.currencyZY = v);
                applyInt(tag, "currencyZW", v -> b.currencyZW = v);
                applyInt(tag, "currencyBX", v -> b.currencyBX = v);
                applyInt(tag, "currencyBY", v -> b.currencyBY = v);
                applyInt(tag, "currencyBW", v -> b.currencyBW = v);
                applyInt(tag, "depositX", v -> b.depositX = v);
                applyInt(tag, "depositY", v -> b.depositY = v);
                applyInt(tag, "withdrawX", v -> b.withdrawX = v);
                applyInt(tag, "withdrawY", v -> b.withdrawY = v);
                applyInt(tag, "exchangeX", v -> b.exchangeX = v);
                applyInt(tag, "exchangeY", v -> b.exchangeY = v);
                applyInt(tag, "claimMailX", v -> b.claimMailX = v);
                applyInt(tag, "claimMailY", v -> b.claimMailY = v);
                applyInt(tag, "refreshX", v -> b.refreshX = v);
                applyInt(tag, "refreshY", v -> b.refreshY = v);
                applyInt(tag, "closeX", v -> b.closeX = v);
                applyInt(tag, "closeY", v -> b.closeY = v);
                applyInt(tag, "headerX", v -> b.headerX = v);
                applyInt(tag, "headerY", v -> b.headerY = v);
                applyInt(tag, "amountLabelX", v -> b.amountLabelX = v);
                applyInt(tag, "amountLabelY", v -> b.amountLabelY = v);
                applyInt(tag, "selectedX", v -> b.selectedX = v);
                applyInt(tag, "selectedY", v -> b.selectedY = v);
                applyInt(tag, "walletZX", v -> b.walletZX = v);
                applyInt(tag, "walletZY", v -> b.walletZY = v);
                applyInt(tag, "walletBX", v -> b.walletBX = v);
                applyInt(tag, "walletBY", v -> b.walletBY = v);
                applyInt(tag, "bankZX", v -> b.bankZX = v);
                applyInt(tag, "bankZY", v -> b.bankZY = v);
                applyInt(tag, "bankBX", v -> b.bankBX = v);
                applyInt(tag, "bankBY", v -> b.bankBY = v);
                applyInt(tag, "mailX", v -> b.mailX = v);
                applyInt(tag, "mailY", v -> b.mailY = v);
                Files.createDirectories(ROOT);
                Files.writeString(BANK_FILE, GSON.toJson(b), StandardCharsets.UTF_8);
                reload();
                return true;
            }
            return false;
        } catch (Exception e) {
            ZEconomy.printStackTrace("Failed to save layout '" + target + "'", e);
            return false;
        }
    }

    public static Map<String, Integer> editableFields(String target, CompoundTag tag) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if ("exchange".equalsIgnoreCase(target)) {
            addField(map, tag, "imageWidth");
            addField(map, tag, "imageHeight");
            addField(map, tag, "table.originX");
            addField(map, tag, "table.originY");
            addField(map, tag, "table.colStep");
            addField(map, tag, "table.rowStep");
            addField(map, tag, "table.templateSlotCol");
            addField(map, tag, "table.inputSlotRow");
            addField(map, tag, "table.outputSlotRow");
            addField(map, tag, "table.textCol");
            addField(map, tag, "table.acceptsLabelRow");
            addField(map, tag, "table.acceptsValueRow");
            addField(map, tag, "table.givesLabelRow");
            addField(map, tag, "table.givesValueRow");
            addField(map, tag, "table.storageCol");
            addField(map, tag, "table.storageLabelRow");
            addField(map, tag, "table.storageRow");
            addField(map, tag, "table.tradeButtonRow");
            addField(map, tag, "tradeButtonW");
            addField(map, tag, "tradeButtonH");
            addField(map, tag, "tradeButtonX");
            addField(map, tag, "playerInvX");
            addField(map, tag, "playerInvY");
            addField(map, tag, "hotbarX");
            addField(map, tag, "hotbarY");
            return map;
        }
        if ("atm".equalsIgnoreCase(target)) {
            addField(map, tag, "imageWidth");
            addField(map, tag, "imageHeight");
            addField(map, tag, "amountX");
            addField(map, tag, "amountY");
            addField(map, tag, "depositZX");
            addField(map, tag, "depositZY");
            addField(map, tag, "withdrawZX");
            addField(map, tag, "withdrawZY");
            addField(map, tag, "depositBX");
            addField(map, tag, "depositBY");
            addField(map, tag, "withdrawBX");
            addField(map, tag, "withdrawBY");
            addField(map, tag, "bankX");
            addField(map, tag, "bankY");
            addField(map, tag, "depositAllZX");
            addField(map, tag, "depositAllZY");
            addField(map, tag, "depositAllBX");
            addField(map, tag, "depositAllBY");
            addField(map, tag, "refreshX");
            addField(map, tag, "refreshY");
            addField(map, tag, "closeX");
            addField(map, tag, "closeY");
            addField(map, tag, "leftButtonX");
            addField(map, tag, "rightButtonX");
            addField(map, tag, "row1Y");
            addField(map, tag, "row2Y");
            addField(map, tag, "row3Y");
            addField(map, tag, "bottomY");
            addField(map, tag, "walletY");
            addField(map, tag, "invY");
            return map;
        }
        if ("bank".equalsIgnoreCase(target)) {
            addField(map, tag, "imageWidth");
            addField(map, tag, "imageHeight");
            addField(map, tag, "amountX");
            addField(map, tag, "amountY");
            addField(map, tag, "currencyZX");
            addField(map, tag, "currencyZY");
            addField(map, tag, "currencyBX");
            addField(map, tag, "currencyBY");
            addField(map, tag, "depositX");
            addField(map, tag, "depositY");
            addField(map, tag, "withdrawX");
            addField(map, tag, "withdrawY");
            addField(map, tag, "exchangeX");
            addField(map, tag, "exchangeY");
            addField(map, tag, "claimMailX");
            addField(map, tag, "claimMailY");
            addField(map, tag, "refreshX");
            addField(map, tag, "refreshY");
            addField(map, tag, "closeX");
            addField(map, tag, "closeY");
            addField(map, tag, "headerX");
            addField(map, tag, "headerY");
            addField(map, tag, "amountLabelX");
            addField(map, tag, "amountLabelY");
            addField(map, tag, "selectedX");
            addField(map, tag, "selectedY");
            addField(map, tag, "walletZX");
            addField(map, tag, "walletZY");
            addField(map, tag, "walletBX");
            addField(map, tag, "walletBY");
            addField(map, tag, "bankZX");
            addField(map, tag, "bankZY");
            addField(map, tag, "bankBX");
            addField(map, tag, "bankBY");
            addField(map, tag, "mailX");
            addField(map, tag, "mailY");
            return map;
        }
        addField(map, tag, "imageWidth");
        addField(map, tag, "imageHeight");
        addField(map, tag, "recipientX");
        addField(map, tag, "recipientY");
        addField(map, tag, "recipientW");
        addField(map, tag, "recipientH");
        addField(map, tag, "sendButtonX");
        addField(map, tag, "sendButtonY");
        addField(map, tag, "claimButtonX");
        addField(map, tag, "claimButtonY");
        addField(map, tag, "buttonW");
        addField(map, tag, "buttonH");
        return map;
    }

    private static void addField(Map<String, Integer> map, CompoundTag tag, String key) {
        if (tag.contains(key)) {
            map.put(key, tag.getInt(key));
        }
    }

    private static void put(CompoundTag tag, String key, int value) {
        tag.putInt(key, value);
    }

    private static void applyInt(CompoundTag tag, String key, IntConsumer consumer) {
        if (tag.contains(key)) {
            consumer.accept(tag.getInt(key));
        }
    }

    private static <T> T loadOrCreate(Path file, Class<T> type, T defaults, T current) throws IOException {
        if (!Files.exists(file)) {
            Files.writeString(file, GSON.toJson(defaults), StandardCharsets.UTF_8);
            return defaults;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            JsonReader reader = new JsonReader(new StringReader(raw));
            reader.setLenient(true);
            T loaded = GSON.fromJson(reader, type);
            if (loaded == null) {
                ZEconomy.LOGGER.warn("[ZEconomy] GUI config '{}' is empty, keeping previous values", file.getFileName());
                return current != null ? current : defaults;
            }
            return loaded;
        } catch (Exception parseError) {
            ZEconomy.LOGGER.warn("[ZEconomy] GUI config '{}' parse error: {}. Keeping previous values and NOT overwriting file.",
                file.getFileName(), parseError.getClass().getSimpleName());
            return current != null ? current : defaults;
        }
    }

    private static void writeReadmeIfMissing() throws IOException {
        if (Files.exists(README_FILE)) {
            return;
        }
        String text = """
            ZEconomy GUI live configs
            Files:
            - gui_atm.json
            - gui_bank.json
            - gui_mailbox.json
            - gui_exchange.json
            - gui_theme.json

            Apply changes in game:
            /zeco reload

            Notes:
            - Coordinates are relative to GUI top-left corner.
            - gui_exchange.json supports table mode:
              set "table.enabled": true and edit row/col values.
              pixelX = originX + col * colStep
              pixelY = originY + row * rowStep
            - Colors in gui_theme.json are hex strings, for example: 0xE61E222A
            - If file is broken, defaults are restored automatically.
            """;
        Files.writeString(README_FILE, text, StandardCharsets.UTF_8);
    }

    public static class AtmLayout {
        public int imageWidth = 240;
        public int imageHeight = 178;
        public int amountX = 16;
        public int amountY = 34;
        public int amountW = 90;
        public int amountH = 18;
        public int depositZX = 16;
        public int depositZY = 58;
        public int withdrawZX = 124;
        public int withdrawZY = 58;
        public int depositBX = 16;
        public int depositBY = 80;
        public int withdrawBX = 124;
        public int withdrawBY = 80;
        public int bankX = 124;
        public int bankY = 80;
        public int depositAllZX = 16;
        public int depositAllZY = 102;
        public int depositAllBX = 124;
        public int depositAllBY = 102;
        public int refreshX = 16;
        public int refreshY = 124;
        public int closeX = 154;
        public int closeY = 124;
        public int leftButtonX = 16;
        public int rightButtonX = 124;
        public int row1Y = 58;
        public int row2Y = 80;
        public int row3Y = 102;
        public int bottomY = 124;
        public int buttonW = 100;
        public int buttonH = 18;
        public int smallButtonW = 70;
        public int closeButtonX = 154;
        public int walletY = 146;
        public int invY = 158;

        public static AtmLayout defaults() {
            return new AtmLayout();
        }

        public int resolvedDepositZX() { return depositZX; }
        public int resolvedDepositZY() { return depositZY; }
        public int resolvedWithdrawZX() { return withdrawZX; }
        public int resolvedWithdrawZY() { return withdrawZY; }
        public int resolvedDepositBX() { return depositBX; }
        public int resolvedDepositBY() { return depositBY; }
        public int resolvedWithdrawBX() { return withdrawBX; }
        public int resolvedWithdrawBY() { return withdrawBY; }
        public int resolvedBankX() { return bankX; }
        public int resolvedBankY() { return bankY; }
        public int resolvedDepositAllZX() { return depositAllZX; }
        public int resolvedDepositAllZY() { return depositAllZY; }
        public int resolvedDepositAllBX() { return depositAllBX; }
        public int resolvedDepositAllBY() { return depositAllBY; }
        public int resolvedRefreshX() { return refreshX; }
        public int resolvedRefreshY() { return refreshY; }
        public int resolvedCloseX() { return closeX; }
        public int resolvedCloseY() { return closeY; }

        public void migrateLegacyIfNeeded() {
            if (usesLegacyGroupedLayout() && !usesNewLayout()) {
                depositZX = leftButtonX;
                depositZY = row1Y;
                withdrawZX = rightButtonX;
                withdrawZY = row1Y;
                depositBX = leftButtonX;
                depositBY = row2Y;
                withdrawBX = rightButtonX;
                withdrawBY = row2Y;
                bankX = rightButtonX;
                bankY = row2Y;
                depositAllZX = leftButtonX;
                depositAllZY = row3Y;
                depositAllBX = rightButtonX;
                depositAllBY = row3Y;
                refreshX = leftButtonX;
                refreshY = bottomY;
                closeX = closeButtonX;
                closeY = bottomY;
            }
        }

        private boolean usesNewLayout() {
            return !(depositZX == 16 && depositZY == 58
                && withdrawZX == 124 && withdrawZY == 58
                && depositBX == 16 && depositBY == 80
                && withdrawBX == 124 && withdrawBY == 80
                && bankX == 124 && bankY == 80
                && depositAllZX == 16 && depositAllZY == 102
                && depositAllBX == 124 && depositAllBY == 102
                && refreshX == 16 && refreshY == 124
                && closeX == 154 && closeY == 124);
        }

        private boolean usesLegacyGroupedLayout() {
            return !(leftButtonX == 16
                && rightButtonX == 124
                && row1Y == 58
                && row2Y == 80
                && row3Y == 102
                && bottomY == 124
                && closeButtonX == 154);
        }
    }

    public static class MailboxLayout {
        public int imageWidth = 176;
        public int imageHeight = 206;
        public int recipientX = 8;
        public int recipientY = 20;
        public int recipientW = 104;
        public int recipientH = 16;
        public int sendButtonX = 116;
        public int sendButtonY = 20;
        public int claimButtonX = 116;
        public int claimButtonY = 40;
        public int buttonW = 52;
        public int buttonH = 18;

        public static MailboxLayout defaults() {
            return new MailboxLayout();
        }
    }

    public static class BankLayout {
        public int imageWidth = 260;
        public int imageHeight = 180;
        public int amountX = 16;
        public int amountY = 34;
        public int amountW = 90;
        public int amountH = 18;
        public int currencyZX = 112;
        public int currencyZY = 34;
        public int currencyZW = 70;
        public int currencyBX = 184;
        public int currencyBY = 34;
        public int currencyBW = 60;
        public int depositX = 16;
        public int depositY = 60;
        public int withdrawX = 92;
        public int withdrawY = 60;
        public int exchangeX = 168;
        public int exchangeY = 60;
        public int claimMailX = 16;
        public int claimMailY = 84;
        public int refreshX = 120;
        public int refreshY = 84;
        public int closeX = 194;
        public int closeY = 84;
        public int headerX = 12;
        public int headerY = 14;
        public int amountLabelX = 16;
        public int amountLabelY = 24;
        public int selectedX = 16;
        public int selectedY = 102;
        public int walletZX = 16;
        public int walletZY = 114;
        public int walletBX = 16;
        public int walletBY = 126;
        public int bankZX = 130;
        public int bankZY = 114;
        public int bankBX = 130;
        public int bankBY = 126;
        public int mailX = 16;
        public int mailY = 138;

        public static BankLayout defaults() {
            return new BankLayout();
        }
    }

    public static class ExchangeLayout {
        public TableGrid table = TableGrid.defaults();
        public int imageWidth = 230;
        public int imageHeight = 228;
        public int templateSlotX = 18;
        public int inputSlotY = 34;
        public int outputSlotY = 64;
        public int textX = 44;
        public int acceptsLabelY = 22;
        public int acceptsValueY = 36;
        public int givesLabelY = 52;
        public int givesValueY = 66;
        public int storageX = 44;
        public int storageLabelY = 74;
        public int storageY = 84;
        public int playerInvX = 8;
        public int playerInvY = 142;
        public int hotbarX = 8;
        public int hotbarY = 200;
        public int tradeButtonX = -1;
        public int tradeButtonY = 49;
        public int tradeButtonW = 54;
        public int tradeButtonH = 20;
        public int maxTextWidth = 170;

        public static ExchangeLayout defaults() {
            return new ExchangeLayout();
        }

        public int resolvedTemplateSlotX() {
            return table.enabled ? table.x(table.templateSlotCol) : templateSlotX;
        }

        public int resolvedInputSlotY() {
            return table.enabled ? table.y(table.inputSlotRow) : inputSlotY;
        }

        public int resolvedOutputSlotY() {
            return table.enabled ? table.y(table.outputSlotRow) : outputSlotY;
        }

        public int resolvedTextX() {
            return table.enabled ? table.x(table.textCol) : textX;
        }

        public int resolvedAcceptsLabelY() {
            return table.enabled ? table.y(table.acceptsLabelRow) : acceptsLabelY;
        }

        public int resolvedAcceptsValueY() {
            return table.enabled ? table.y(table.acceptsValueRow) : acceptsValueY;
        }

        public int resolvedGivesLabelY() {
            return table.enabled ? table.y(table.givesLabelRow) : givesLabelY;
        }

        public int resolvedGivesValueY() {
            return table.enabled ? table.y(table.givesValueRow) : givesValueY;
        }

        public int resolvedStorageX() {
            return table.enabled ? table.x(table.storageCol) : storageX;
        }

        public int resolvedStorageLabelY() {
            return table.enabled ? table.y(table.storageLabelRow) : storageLabelY;
        }

        public int resolvedStorageY() {
            return table.enabled ? table.y(table.storageRow) : storageY;
        }

        public int resolvedTradeButtonY() {
            return table.enabled ? table.y(table.tradeButtonRow) : tradeButtonY;
        }

        public int resolvedTradeButtonX() {
            return tradeButtonX >= 0 ? tradeButtonX : imageWidth - tradeButtonW - 8;
        }

        public static class TableGrid {
            public boolean enabled = true;
            public int originX = 8;
            public int originY = 8;
            public int colStep = 18;
            public int rowStep = 14;
            public int templateSlotCol = 0;
            public int inputSlotRow = 2;
            public int outputSlotRow = 4;
            public int textCol = 2;
            public int acceptsLabelRow = 1;
            public int acceptsValueRow = 2;
            public int givesLabelRow = 3;
            public int givesValueRow = 4;
            public int storageCol = 2;
            public int storageLabelRow = 5;
            public int storageRow = 6;
            public int tradeButtonRow = 3;

            public int x(int col) {
                return originX + col * colStep;
            }

            public int y(int row) {
                return originY + row * rowStep;
            }

            public static TableGrid defaults() {
                return new TableGrid();
            }
        }
    }

    public static class GuiTheme {
        public String backgroundStart = "0xE61E222A";
        public String backgroundEnd = "0xE612151C";
        public String headerPanel = "0xFF2F3A47";
        public String sectionPanel = "0x662B313A";
        public String sectionPanelAlt = "0x44101725";
        public String slotOuter = "0xAA0E131B";
        public String slotInner = "0x661E2A39";
        public String slotLight = "0x99AAB6C7";
        public String slotDark = "0x8839455A";
        public String titleColor = "0xEAF2FF";
        public String labelColor = "0xC8D2E0";
        public String valueColor = "0xD6DFED";
        public String mutedColor = "0xAEB8C6";
        public String atmWalletLeftColor = "0xB6D3FF";
        public String atmWalletRightColor = "0xF4D37D";
        public String atmInvLeftColor = "0xA9D9F5";
        public String atmInvRightColor = "0xF2D69D";

        public static GuiTheme defaults() {
            return new GuiTheme();
        }

        public static int parseColor(String value, int fallback) {
            if (value == null || value.isBlank()) {
                return fallback;
            }
            try {
                String v = value.trim();
                if (v.startsWith("0x") || v.startsWith("0X")) {
                    return (int) Long.parseLong(v.substring(2), 16);
                }
                if (v.startsWith("#")) {
                    return (int) Long.parseLong(v.substring(1), 16);
                }
                return Integer.parseInt(v);
            } catch (Exception ignored) {
                return fallback;
            }
        }
    }
}
