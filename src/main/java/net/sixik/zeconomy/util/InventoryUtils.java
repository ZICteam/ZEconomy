package net.sixik.zeconomy.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryUtils {
    private InventoryUtils() {
    }

    public static int countItem(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean removeItem(Player player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() != item) {
                continue;
            }
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    public static void giveItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        boolean inserted = player.getInventory().add(stack.copy());
        if (!inserted) {
            player.drop(stack.copy(), false);
        }
    }
}
