package example.integration;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import io.zicteam.zeconomy.api.ZEconomyApi;
import io.zicteam.zeconomy.api.ZEconomyApiProvider;
import io.zicteam.zeconomy.api.event.BalanceChangeEvent;
import io.zicteam.zeconomy.api.event.DailyRewardEvent;

@Mod("example_zeconomy_addon")
public final class ExampleEconomyAddon {
    private final ZEconomyApi economyApi;

    public ExampleEconomyAddon() {
        this.economyApi = ZEconomyApiProvider.get();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ExampleEconomyCommands.register(event.getDispatcher(), economyApi);
    }

    @SubscribeEvent
    public void onBalanceChange(BalanceChangeEvent event) {
        if ("z_coin".equals(event.getCurrencyId())) {
            System.out.println(
                "[ExampleAddon] Balance change: player=" + event.getPlayerId()
                    + " delta=" + event.getDelta()
                    + " new=" + event.getNewBalance()
                    + " reason=" + event.getReason()
            );
        }
    }

    @SubscribeEvent
    public void onDailyReward(DailyRewardEvent event) {
        System.out.println(
            "[ExampleAddon] Daily reward: player=" + event.getPlayerId()
                + " z=" + event.getZReward()
                + " b=" + event.getBReward()
                + " streak=" + event.getStreak()
        );
    }
}
