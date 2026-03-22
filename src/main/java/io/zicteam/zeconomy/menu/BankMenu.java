package io.zicteam.zeconomy.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import io.zicteam.zeconomy.content.EconomyContent;

public class BankMenu extends AbstractContainerMenu {
    public BankMenu(int containerId, Inventory inventory) {
        super(EconomyContent.BANK_MENU.get(), containerId);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
