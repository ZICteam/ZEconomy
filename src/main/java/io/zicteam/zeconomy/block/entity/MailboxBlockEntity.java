package io.zicteam.zeconomy.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import io.zicteam.zeconomy.content.EconomyContent;

public class MailboxBlockEntity extends BlockEntity {
    private final SimpleContainer sendInventory = new SimpleContainer(9);

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(EconomyContent.MAILBOX_BLOCK_ENTITY.get(), pos, state);
    }

    public SimpleContainer getSendInventory() {
        return sendInventory;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        NonNullList<net.minecraft.world.item.ItemStack> items = NonNullList.withSize(9, net.minecraft.world.item.ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            items.set(i, sendInventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        NonNullList<net.minecraft.world.item.ItemStack> tmp = NonNullList.withSize(9, net.minecraft.world.item.ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, tmp);
        for (int i = 0; i < 9; i++) {
            sendInventory.setItem(i, tmp.get(i));
        }
    }
}
