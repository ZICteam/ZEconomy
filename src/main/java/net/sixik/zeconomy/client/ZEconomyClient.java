package net.sixik.zeconomy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.sixik.zeconomy.ZEconomy;
import net.sixik.zeconomy.client.screen.AtmScreen;
import net.sixik.zeconomy.client.screen.BankScreen;
import net.sixik.zeconomy.client.screen.ExchangeScreen;
import net.sixik.zeconomy.client.screen.MailboxScreen;
import net.sixik.zeconomy.content.EconomyContent;

@Mod.EventBusSubscriber(modid = ZEconomy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZEconomyClient {
    @SubscribeEvent
    public static void onRegisterScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(EconomyContent.BANK_MENU.get(), BankScreen::new);
            MenuScreens.register(EconomyContent.ATM_MENU.get(), AtmScreen::new);
            MenuScreens.register(EconomyContent.MAILBOX_MENU.get(), MailboxScreen::new);
            MenuScreens.register(EconomyContent.EXCHANGE_MENU.get(), ExchangeScreen::new);
        });
    }
}
