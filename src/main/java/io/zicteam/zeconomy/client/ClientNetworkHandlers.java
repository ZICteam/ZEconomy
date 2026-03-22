package io.zicteam.zeconomy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import io.zicteam.zeconomy.CustomPlayerData;
import io.zicteam.zeconomy.client.screen.GuiLayoutEditorScreen;
import io.zicteam.zeconomy.currencies.data.CurrencyData;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void handleSync(CompoundTag currencies, CompoundTag playerCurrencies, CompoundTag customData) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        CurrencyData.CLIENT.reloadCurrenciesFromNetwork(currencies);
        CurrencyPlayerData.CLIENT.load(playerCurrencies);
        CustomPlayerData.CLIENT = new CustomPlayerData.Client(new CustomPlayerData.Data(customData.copy()));
    }

    public static void openLayoutEditor(String target, CompoundTag values) {
        Minecraft.getInstance().setScreen(new GuiLayoutEditorScreen(target, values));
    }
}
