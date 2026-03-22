package net.sixik.zeconomy.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.sixik.zeconomy.block.entity.MailboxBlockEntity;
import net.sixik.zeconomy.content.EconomyContent;

public class MailboxMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;

    public MailboxMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(9), ContainerLevelAccess.NULL, BlockPos.ZERO);
    }

    public MailboxMenu(int containerId, Inventory inventory, MailboxBlockEntity blockEntity) {
        this(containerId, inventory, blockEntity.getSendInventory(), ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), blockEntity.getBlockPos());
    }

    private MailboxMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access, BlockPos pos) {
        super(EconomyContent.MAILBOX_MENU.get(), containerId);
        this.container = container;
        this.access = access;
        this.blockPos = pos;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(container, col + row * 3, 62 + col * 18, 50 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 116 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 174));
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> level.getBlockState(pos).is(EconomyContent.MAILBOX_BLOCK.get()), true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            if (slotIndex < 9) {
                if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(stack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }
}
