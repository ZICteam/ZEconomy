package net.sixik.zeconomy.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.sixik.zeconomy.content.EconomyContent;

public class AtmMenu extends AbstractContainerMenu {
    public AtmMenu(int containerId, Inventory inventory) {
        super(EconomyContent.ATM_MENU.get(), containerId);
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
