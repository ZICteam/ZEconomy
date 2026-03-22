package net.sixik.zeconomy.block.entity;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.sixik.zeconomy.content.EconomyContent;

public class ExchangeBlockEntity extends BlockEntity {
    public static final int SLOT_INPUT_TEMPLATE = 0;
    public static final int SLOT_OUTPUT_TEMPLATE = 1;
    public static final int STORAGE_START = 2;
    public static final int STORAGE_SIZE = 27;
    public static final int TOTAL_SLOTS = STORAGE_START + STORAGE_SIZE;

    private UUID owner;
    private final SimpleContainer inventory = new SimpleContainer(TOTAL_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            ExchangeBlockEntity.this.setChanged();
        }
    };

    public ExchangeBlockEntity(BlockPos pos, BlockState state) {
        super(EconomyContent.EXCHANGE_BLOCK_ENTITY.get(), pos, state);
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public ItemStack getInputTemplate() {
        return inventory.getItem(SLOT_INPUT_TEMPLATE);
    }

    public ItemStack getOutputTemplate() {
        return inventory.getItem(SLOT_OUTPUT_TEMPLATE);
    }

    public String getInputItemId() {
        ItemStack stack = getInputTemplate();
        if (stack.isEmpty()) {
            return "minecraft:air";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public int getInputCount() {
        return Math.max(1, getInputTemplate().getCount());
    }

    public String getOutputItemId() {
        ItemStack stack = getOutputTemplate();
        if (stack.isEmpty()) {
            return "minecraft:air";
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public int getOutputCount() {
        return Math.max(1, getOutputTemplate().getCount());
    }

    public void setOffer(String inputItemId, int inputCount, String outputItemId, int outputCount) {
        Item input = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(inputItemId));
        Item output = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(outputItemId));
        if (input != null && input != net.minecraft.world.item.Items.AIR) {
            inventory.setItem(SLOT_INPUT_TEMPLATE, new ItemStack(input, Math.max(1, inputCount)));
        }
        if (output != null && output != net.minecraft.world.item.Items.AIR) {
            inventory.setItem(SLOT_OUTPUT_TEMPLATE, new ItemStack(output, Math.max(1, outputCount)));
        }
        setChanged();
    }

    public int countStorageItem(Item item) {
        int total = 0;
        for (int i = STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public int freeSpaceFor(Item item) {
        int free = 0;
        for (int i = STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                free += item.getDefaultInstance().getMaxStackSize();
                continue;
            }
            if (stack.getItem() == item) {
                free += (stack.getMaxStackSize() - stack.getCount());
            }
        }
        return free;
    }

    public boolean insertToStorage(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        ItemStack remaining = stack.copy();
        for (int i = STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty()) {
                int move = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inventory.setItem(i, new ItemStack(remaining.getItem(), move));
                remaining.shrink(move);
                if (remaining.isEmpty()) {
                    setChanged();
                    return true;
                }
                continue;
            }
            if (ItemStack.isSameItemSameTags(slot, remaining) && slot.getCount() < slot.getMaxStackSize()) {
                int move = Math.min(remaining.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                remaining.shrink(move);
                if (remaining.isEmpty()) {
                    setChanged();
                    return true;
                }
            }
        }
        setChanged();
        return remaining.isEmpty();
    }

    public boolean extractFromStorage(Item item, int count) {
        int remaining = count;
        for (int i = STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack slot = inventory.getItem(i);
            if (slot.isEmpty() || slot.getItem() != item) {
                continue;
            }
            int take = Math.min(remaining, slot.getCount());
            slot.shrink(take);
            if (slot.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            remaining -= take;
            if (remaining <= 0) {
                setChanged();
                return true;
            }
        }
        setChanged();
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
