package io.zicteam.zeconomy.api.event;

import net.minecraftforge.common.MinecraftForge;

public final class ZEconomyApiEvents {
    private ZEconomyApiEvents() {
    }

    public static void post(ZEconomyApiEvent event) {
        MinecraftForge.EVENT_BUS.post(event);
    }
}
