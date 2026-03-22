package net.sixik.zeconomy.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sixik.zeconomy.block.entity.ExchangeBlockEntity;
import net.sixik.zeconomy.config.GuiLayoutConfig;
import net.sixik.zeconomy.content.EconomyContent;

public class ExchangeMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final Player player;

    public ExchangeMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, data != null ? data.readBlockPos() : BlockPos.ZERO);
    }

    public ExchangeMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(EconomyContent.EXCHANGE_MENU.get(), containerId);
        GuiLayoutConfig.reload();
        GuiLayoutConfig.ExchangeLayout layout = GuiLayoutConfig.exchange();
        this.player = inventory.player;
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        Container tmp = new SimpleContainer(ExchangeBlockEntity.TOTAL_SLOTS);
        if (inventory.player.level().getBlockEntity(pos) instanceof ExchangeBlockEntity exchange) {
            tmp = exchange.getInventory();
        }
        this.container = tmp;
        this.container.startOpen(inventory.player);

        this.addSlot(new TemplateSlot(container, ExchangeBlockEntity.SLOT_INPUT_TEMPLATE, layout.resolvedTemplateSlotX(), layout.resolvedInputSlotY()));
        this.addSlot(new TemplateSlot(container, ExchangeBlockEntity.SLOT_OUTPUT_TEMPLATE, layout.resolvedTemplateSlotX(), layout.resolvedOutputSlotY()));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = ExchangeBlockEntity.STORAGE_START + col + row * 9;
                this.addSlot(new StorageSlot(container, slot, layout.resolvedStorageX() + col * 18, layout.resolvedStorageY() + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, layout.playerInvX + col * 18, layout.playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, layout.hotbarX + col * 18, layout.hotbarY));
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean isOwner() {
        if (player.hasPermissions(2)) {
            return true;
        }
        if (!(player.level().getBlockEntity(blockPos) instanceof ExchangeBlockEntity exchange)) {
            return false;
        }
        return exchange.getOwner() != null && exchange.getOwner().equals(player.getUUID());
    }

    public ItemStack getInputTemplateStack() {
        return container.getItem(ExchangeBlockEntity.SLOT_INPUT_TEMPLATE);
    }

    public ItemStack getOutputTemplateStack() {
        return container.getItem(ExchangeBlockEntity.SLOT_OUTPUT_TEMPLATE);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).is(EconomyContent.EXCHANGE_BLOCK.get()), true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }

    private class TemplateSlot extends Slot {
        private TemplateSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isOwner();
        }

        @Override
        public boolean mayPickup(Player player) {
            return isOwner();
        }
    }

    private class StorageSlot extends Slot {
        private StorageSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isOwner();
        }

        @Override
        public boolean mayPickup(Player player) {
            return isOwner();
        }
    }
}
