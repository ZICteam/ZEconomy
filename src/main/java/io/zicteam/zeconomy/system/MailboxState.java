package io.zicteam.zeconomy.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

final class MailboxState {
    private final Map<UUID, List<ItemStack>> mailboxByPlayer = new HashMap<>();

    void send(UUID target, ItemStack stack) {
        if (target == null || stack == null || stack.isEmpty()) {
            return;
        }
        mailboxByPlayer.computeIfAbsent(target, ignored -> new ArrayList<>()).add(stack.copy());
        DataStorageManager.markDirty();
    }

    List<ItemStack> claim(UUID playerId) {
        List<ItemStack> items = mailboxByPlayer.remove(playerId);
        int count = items == null ? 0 : items.size();
        if (count > 0) {
            DataStorageManager.markDirty();
        }
        return items == null ? List.of() : items;
    }

    int pendingCount(UUID playerId) {
        return mailboxByPlayer.getOrDefault(playerId, List.of()).size();
    }

    void clear() {
        mailboxByPlayer.clear();
    }

    void writeTo(CompoundTag root) {
        ListTag mails = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> mail : mailboxByPlayer.entrySet()) {
            CompoundTag playerMail = new CompoundTag();
            playerMail.putUUID("uuid", mail.getKey());
            ListTag items = new ListTag();
            for (ItemStack stack : mail.getValue()) {
                items.add(stack.save(new CompoundTag()));
            }
            playerMail.put("items", items);
            mails.add(playerMail);
        }
        root.put("mail", mails);
    }

    void readFrom(CompoundTag root) {
        clear();
        for (Tag tag : root.getList("mail", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag mailTag)) {
                continue;
            }
            UUID id = mailTag.getUUID("uuid");
            List<ItemStack> items = new ArrayList<>();
            for (Tag itemTag : mailTag.getList("items", Tag.TAG_COMPOUND)) {
                if (itemTag instanceof CompoundTag stackTag) {
                    items.add(ItemStack.of(stackTag));
                }
            }
            mailboxByPlayer.put(id, items);
        }
    }
}
