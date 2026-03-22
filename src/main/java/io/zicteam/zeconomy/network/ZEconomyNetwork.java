package io.zicteam.zeconomy.network;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.block.entity.ExchangeBlockEntity;
import io.zicteam.zeconomy.block.entity.MailboxBlockEntity;
import io.zicteam.zeconomy.config.GuiLayoutConfig;
import io.zicteam.zeconomy.content.EconomyContent;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.menu.MailboxMenu;
import io.zicteam.zeconomy.util.InventoryUtils;
import io.zicteam.zeconomy.utils.CurrencyHelper;

public class ZEconomyNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(ZEconomy.MOD_ID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    public ZEconomyNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, SyncPayloadS2C.class, SyncPayloadS2C::encode, SyncPayloadS2C::decode, SyncPayloadS2C::handle);
        CHANNEL.registerMessage(1, BankActionC2S.class, BankActionC2S::encode, BankActionC2S::decode, BankActionC2S::handle);
        CHANNEL.registerMessage(2, MailboxActionC2S.class, MailboxActionC2S::encode, MailboxActionC2S::decode, MailboxActionC2S::handle);
        CHANNEL.registerMessage(3, ExchangeTradeC2S.class, ExchangeTradeC2S::encode, ExchangeTradeC2S::decode, ExchangeTradeC2S::handle);
        CHANNEL.registerMessage(4, OpenLayoutEditorS2C.class, OpenLayoutEditorS2C::encode, OpenLayoutEditorS2C::decode, OpenLayoutEditorS2C::handle);
        CHANNEL.registerMessage(5, SaveLayoutEditorC2S.class, SaveLayoutEditorC2S::encode, SaveLayoutEditorC2S::decode, SaveLayoutEditorC2S::handle);
    }

    public static void syncPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CurrencyPlayerData.SERVER.newPlayer(player);
        CustomPlayerData.SERVER.createData(player);
        SyncPayloadS2C payload = new SyncPayloadS2C(
            CurrencyData.SERVER.serialize(),
            CurrencyPlayerData.SERVER.getPlayersCurrency(player).stream().collect(CompoundCollectors.toPlayerCurrencyTag()),
            CustomPlayerData.SERVER.getPlayerCustomData(player).nbt.copy()
        );
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncPlayer(player);
        }
    }

    public static void sendBankAction(String action, String currency, double amount) {
        CHANNEL.sendToServer(new BankActionC2S(action, currency, amount));
    }

    public static void sendMailboxAction(String action, BlockPos pos, String recipient) {
        CHANNEL.sendToServer(new MailboxActionC2S(action, pos, recipient));
    }

    public static void sendExchangeTrade(BlockPos pos) {
        CHANNEL.sendToServer(new ExchangeTradeC2S(pos));
    }

    public static void openLayoutEditor(ServerPlayer player, String target) {
        if (player == null || !GuiLayoutConfig.isValidTarget(target)) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenLayoutEditorS2C(target, GuiLayoutConfig.toTag(target)));
    }

    public static void sendLayoutEditorSave(String target, CompoundTag values) {
        CHANNEL.sendToServer(new SaveLayoutEditorC2S(target, values));
    }

    private static class SyncPayloadS2C {
        private final CompoundTag currencies;
        private final CompoundTag playerCurrencies;
        private final CompoundTag customData;

        private SyncPayloadS2C(CompoundTag currencies, CompoundTag playerCurrencies, CompoundTag customData) {
            this.currencies = currencies;
            this.playerCurrencies = playerCurrencies;
            this.customData = customData;
        }

        private static void encode(SyncPayloadS2C payload, FriendlyByteBuf buf) {
            buf.writeNbt(payload.currencies);
            buf.writeNbt(payload.playerCurrencies);
            buf.writeNbt(payload.customData);
        }

        private static SyncPayloadS2C decode(FriendlyByteBuf buf) {
            CompoundTag currencies = buf.readNbt();
            CompoundTag playerCurrencies = buf.readNbt();
            CompoundTag customData = buf.readNbt();
            return new SyncPayloadS2C(currencies == null ? new CompoundTag() : currencies, playerCurrencies == null ? new CompoundTag() : playerCurrencies, customData == null ? new CompoundTag() : customData);
        }

        private static void handle(SyncPayloadS2C payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                io.zicteam.zeconomy.client.ClientNetworkHandlers.handleSync(payload.currencies, payload.playerCurrencies, payload.customData)
            ));
            context.setPacketHandled(true);
        }
    }

    private static class MailboxActionC2S {
        private final String action;
        private final BlockPos pos;
        private final String recipient;

        private MailboxActionC2S(String action, BlockPos pos, String recipient) {
            this.action = action == null ? "" : action;
            this.pos = pos == null ? BlockPos.ZERO : pos;
            this.recipient = recipient == null ? "" : recipient;
        }

        private static void encode(MailboxActionC2S payload, FriendlyByteBuf buf) {
            buf.writeUtf(payload.action);
            buf.writeBlockPos(payload.pos);
            buf.writeUtf(payload.recipient);
        }

        private static MailboxActionC2S decode(FriendlyByteBuf buf) {
            return new MailboxActionC2S(buf.readUtf(), buf.readBlockPos(), buf.readUtf());
        }

        private static void handle(MailboxActionC2S payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if ("MAILBOX_CLAIM".equals(payload.action)) {
                    for (ItemStack stack : ZEconomy.EXTRA_DATA.claimMail(player.getUUID())) {
                        InventoryUtils.giveItem(player, stack);
                    }
                    ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
                    syncPlayer(player);
                    return;
                }
                if (!"MAILBOX_SEND".equals(payload.action)) {
                    return;
                }
                if (!(player.containerMenu instanceof MailboxMenu openMenu)) {
                    return;
                }
                if (!(player.level().getBlockEntity(openMenu.getBlockPos()) instanceof MailboxBlockEntity mailbox)) {
                    return;
                }
                if (player.getServer() == null) {
                    return;
                }
                ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(payload.recipient);
                if (target == null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.mailbox.player_not_found", payload.recipient));
                    return;
                }
                int sent = 0;
                for (int i = 0; i < mailbox.getSendInventory().getContainerSize(); i++) {
                    ItemStack stack = mailbox.getSendInventory().getItem(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ZEconomy.EXTRA_DATA.sendMail(target.getUUID(), stack.copy());
                    mailbox.getSendInventory().setItem(i, ItemStack.EMPTY);
                    sent++;
                }
                if (sent > 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.mailbox.sent", sent, target.getName().getString()));
                    target.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.mailbox.received", player.getName().getString()));
                    ZEconomy.EXTRA_DATA.syncPlayerMirror(target);
                    ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
                    syncPlayer(target);
                    syncPlayer(player);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static class BankActionC2S {
        private final String action;
        private final String currency;
        private final double amount;

        private BankActionC2S(String action, String currency, double amount) {
            this.action = action == null ? "" : action;
            this.currency = currency == null ? "z_coin" : currency;
            this.amount = amount;
        }

        private static void encode(BankActionC2S payload, FriendlyByteBuf buf) {
            buf.writeUtf(payload.action);
            buf.writeUtf(payload.currency);
            buf.writeDouble(payload.amount);
        }

        private static BankActionC2S decode(FriendlyByteBuf buf) {
            return new BankActionC2S(buf.readUtf(), buf.readUtf(), buf.readDouble());
        }

        private static void handle(BankActionC2S payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                switch (payload.action) {
                    case "BANK_DEPOSIT" -> ZEconomy.EXTRA_DATA.depositToBank(player, payload.currency, payload.amount);
                    case "BANK_WITHDRAW" -> ZEconomy.EXTRA_DATA.withdrawFromBank(player, payload.currency, payload.amount);
                    case "EXCHANGE_B_TO_Z" -> ZEconomy.EXTRA_DATA.exchangeCurrency(player, "b_coin", "z_coin", payload.amount);
                    case "MAIL_CLAIM" -> {
                        for (ItemStack stack : ZEconomy.EXTRA_DATA.claimMail(player.getUUID())) {
                            InventoryUtils.giveItem(player, stack);
                        }
                    }
                    case "SYNC" -> {
                    }
                case "ATM_DEPOSIT_Z" -> depositPhysical(player, "z_coin", EconomyContent.Z_COIN_ITEM.get(), payload.amount);
                    case "ATM_DEPOSIT_B" -> depositPhysical(player, "b_coin", EconomyContent.B_COIN_ITEM.get(), payload.amount);
                case "ATM_DEPOSIT_ALL_Z" -> depositPhysical(player, "z_coin", EconomyContent.Z_COIN_ITEM.get(), -1);
                    case "ATM_DEPOSIT_ALL_B" -> depositPhysical(player, "b_coin", EconomyContent.B_COIN_ITEM.get(), -1);
                case "ATM_WITHDRAW_Z" -> withdrawPhysical(player, "z_coin", EconomyContent.Z_COIN_ITEM.get(), payload.amount);
                    default -> {
                    }
                }
                ZEconomy.EXTRA_DATA.syncPlayerMirror(player);
                syncPlayer(player);
            });
            context.setPacketHandled(true);
        }

        private static void depositPhysical(ServerPlayer player, String currency, net.minecraft.world.item.Item item, double amount) {
            if (!EconomyConfig.ENABLE_PHYSICAL_MONEY.get()) {
                return;
            }
            int toDeposit = amount < 0 ? InventoryUtils.countItem(player, item) : (int) Math.floor(amount);
            if (toDeposit <= 0) {
                return;
            }
            int has = InventoryUtils.countItem(player, item);
            toDeposit = Math.min(toDeposit, has);
            if (toDeposit <= 0) {
                return;
            }
            boolean removed = InventoryUtils.removeItem(player, item, toDeposit);
            if (!removed) {
                return;
            }
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, toDeposit);
        }

        private static void withdrawPhysical(ServerPlayer player, String currency, net.minecraft.world.item.Item item, double amount) {
            if (!EconomyConfig.ENABLE_PHYSICAL_MONEY.get()) {
                return;
            }
            int toWithdraw = amount <= 0 ? EconomyConfig.ATM_WITHDRAW_STEP.get().intValue() : (int) Math.floor(amount);
            if (toWithdraw <= 0) {
                return;
            }
            double balance = CurrencyHelper.getPlayerCurrencyServerData().getBalance(player, currency).value;
            if (balance < toWithdraw) {
                return;
            }
            CurrencyHelper.getPlayerCurrencyServerData().addCurrencyValue(player, currency, -toWithdraw);
            InventoryUtils.giveItem(player, new ItemStack(item, toWithdraw));
        }
    }

    private static class ExchangeTradeC2S {
        private final BlockPos pos;

        private ExchangeTradeC2S(BlockPos pos) {
            this.pos = pos == null ? BlockPos.ZERO : pos;
        }

        private static void encode(ExchangeTradeC2S payload, FriendlyByteBuf buf) {
            buf.writeBlockPos(payload.pos);
        }

        private static ExchangeTradeC2S decode(FriendlyByteBuf buf) {
            return new ExchangeTradeC2S(buf.readBlockPos());
        }

        private static void handle(ExchangeTradeC2S payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }
                if (!(player.level().getBlockEntity(payload.pos) instanceof ExchangeBlockEntity exchange)) {
                    return;
                }
                ItemStack inputTemplate = exchange.getInputTemplate().copy();
                ItemStack outputTemplate = exchange.getOutputTemplate().copy();
                if (inputTemplate.isEmpty() || outputTemplate.isEmpty()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.invalid_offer"));
                    return;
                }
                if (exchange.countStorageItem(outputTemplate.getItem()) < outputTemplate.getCount()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.out_of_stock"));
                    return;
                }
                if (InventoryUtils.countItem(player, inputTemplate.getItem()) < inputTemplate.getCount()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.not_enough_items"));
                    return;
                }
                if (exchange.freeSpaceFor(inputTemplate.getItem()) < inputTemplate.getCount()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.block_full"));
                    return;
                }
                if (!canPlayerReceive(player, outputTemplate.copy())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.player_full"));
                    return;
                }
                if (!InventoryUtils.removeItem(player, inputTemplate.getItem(), inputTemplate.getCount())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.not_enough_items"));
                    return;
                }
                if (!exchange.insertToStorage(new ItemStack(inputTemplate.getItem(), inputTemplate.getCount()))) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.block_full"));
                    return;
                }
                if (!exchange.extractFromStorage(outputTemplate.getItem(), outputTemplate.getCount())) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.out_of_stock"));
                    return;
                }
                if (!insertIntoPlayerInventory(player, outputTemplate.copy())) {
                    exchange.insertToStorage(outputTemplate.copy());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.player_full"));
                    return;
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.zeconomy.exchange.completed"));
            });
            context.setPacketHandled(true);
        }

        private static boolean canPlayerReceive(ServerPlayer player, ItemStack stack) {
            int need = stack.getCount();
            for (ItemStack slot : player.getInventory().items) {
                if (slot.isEmpty()) {
                    need -= Math.min(stack.getMaxStackSize(), need);
                } else if (ItemStack.isSameItemSameTags(slot, stack)) {
                    need -= Math.min(slot.getMaxStackSize() - slot.getCount(), need);
                }
                if (need <= 0) {
                    return true;
                }
            }
            return need <= 0;
        }

        private static boolean insertIntoPlayerInventory(ServerPlayer player, ItemStack stack) {
            ItemStack remaining = stack.copy();
            for (ItemStack slot : player.getInventory().items) {
                if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                    int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                    slot.grow(move);
                    remaining.shrink(move);
                    if (remaining.isEmpty()) {
                        return true;
                    }
                }
            }
            for (int i = 0; i < player.getInventory().items.size(); i++) {
                ItemStack slot = player.getInventory().items.get(i);
                if (slot.isEmpty()) {
                    player.getInventory().items.set(i, remaining.copy());
                    return true;
                }
            }
            return false;
        }
    }

    private static class OpenLayoutEditorS2C {
        private final String target;
        private final CompoundTag values;

        private OpenLayoutEditorS2C(String target, CompoundTag values) {
            this.target = target == null ? "exchange" : target;
            this.values = values == null ? new CompoundTag() : values.copy();
        }

        private static void encode(OpenLayoutEditorS2C payload, FriendlyByteBuf buf) {
            buf.writeUtf(payload.target);
            buf.writeNbt(payload.values);
        }

        private static OpenLayoutEditorS2C decode(FriendlyByteBuf buf) {
            String target = buf.readUtf();
            CompoundTag values = buf.readNbt();
            return new OpenLayoutEditorS2C(target, values == null ? new CompoundTag() : values);
        }

        private static void handle(OpenLayoutEditorS2C payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                io.zicteam.zeconomy.client.ClientNetworkHandlers.openLayoutEditor(payload.target, payload.values)
            ));
            context.setPacketHandled(true);
        }
    }

    private static class SaveLayoutEditorC2S {
        private final String target;
        private final CompoundTag values;

        private SaveLayoutEditorC2S(String target, CompoundTag values) {
            this.target = target == null ? "" : target;
            this.values = values == null ? new CompoundTag() : values.copy();
        }

        private static void encode(SaveLayoutEditorC2S payload, FriendlyByteBuf buf) {
            buf.writeUtf(payload.target);
            buf.writeNbt(payload.values);
        }

        private static SaveLayoutEditorC2S decode(FriendlyByteBuf buf) {
            String target = buf.readUtf();
            CompoundTag values = buf.readNbt();
            return new SaveLayoutEditorC2S(target, values == null ? new CompoundTag() : values);
        }

        private static void handle(SaveLayoutEditorC2S payload, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !player.hasPermissions(2)) {
                    return;
                }
                boolean ok = GuiLayoutConfig.applyTagAndSave(payload.target, payload.values);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(ok ? "GUI layout saved: " + payload.target : "Failed to save GUI layout: " + payload.target));
            });
            context.setPacketHandled(true);
        }
    }

    private static final class CompoundCollectors {
        private CompoundCollectors() {
        }

        private static java.util.stream.Collector<CurrencyPlayerData.PlayerCurrency, ?, CompoundTag> toPlayerCurrencyTag() {
            return java.util.stream.Collector.of(
                CompoundTag::new,
                (root, currency) -> {
                    net.minecraft.nbt.ListTag list = root.contains("currencies", net.minecraft.nbt.Tag.TAG_LIST)
                        ? root.getList("currencies", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        : new net.minecraft.nbt.ListTag();
                    list.add(currency.serialize());
                    root.put("currencies", list);
                },
                (left, right) -> {
                    net.minecraft.nbt.ListTag leftList = left.contains("currencies", net.minecraft.nbt.Tag.TAG_LIST)
                        ? left.getList("currencies", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        : new net.minecraft.nbt.ListTag();
                    net.minecraft.nbt.ListTag rightList = right.contains("currencies", net.minecraft.nbt.Tag.TAG_LIST)
                        ? right.getList("currencies", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        : new net.minecraft.nbt.ListTag();
                    leftList.addAll(rightList);
                    left.put("currencies", leftList);
                    return left;
                }
            );
        }
    }
}
