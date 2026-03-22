package io.zicteam.zeconomy.forge;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.content.EconomyContent;

@Mod(ZEconomy.MOD_ID)
public final class ZEconomyForge {
    public ZEconomyForge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC);
        EconomyContent.register(FMLJavaModLoadingContext.get().getModEventBus());
        ZEconomy.init();
    }
}
