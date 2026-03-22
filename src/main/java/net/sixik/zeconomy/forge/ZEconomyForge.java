package net.sixik.zeconomy.forge;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sixik.zeconomy.ZEconomy;
import net.sixik.zeconomy.config.EconomyConfig;
import net.sixik.zeconomy.content.EconomyContent;

@Mod(ZEconomy.MOD_ID)
public final class ZEconomyForge {
    public ZEconomyForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        EconomyContent.register(FMLJavaModLoadingContext.get().getModEventBus());
        ZEconomy.init();
    }
}
