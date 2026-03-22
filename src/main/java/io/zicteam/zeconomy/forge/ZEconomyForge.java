package io.zicteam.zeconomy.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import io.zicteam.zeconomy.ZEconomy;
import io.zicteam.zeconomy.config.EconomyConfig;
import io.zicteam.zeconomy.content.EconomyContent;

@Mod(ZEconomy.MOD_ID)
public final class ZEconomyForge {
    private static final String COMMON_CONFIG_PATH = ZEconomy.MOD_ID + "/" + ZEconomy.MOD_ID + "-common.toml";

    public ZEconomyForge(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, EconomyConfig.SPEC, COMMON_CONFIG_PATH);
        EconomyContent.register(context.getModEventBus());
        ZEconomy.init();
    }
}
