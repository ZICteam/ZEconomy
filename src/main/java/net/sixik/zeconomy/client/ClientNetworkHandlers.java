package net.sixik.zeconomy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.sixik.zeconomy.CustomPlayerData;
import net.sixik.zeconomy.client.screen.GuiLayoutEditorScreen;
import net.sixik.zeconomy.currencies.data.CurrencyData;
import net.sixik.zeconomy.currencies.data.CurrencyPlayerData;

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
