package io.zicteam.zeconomy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import io.zicteam.zeconomy.config.GuiLayoutConfig;
import io.zicteam.zeconomy.content.EconomyContent;
import io.zicteam.zeconomy.currencies.data.CurrencyPlayerData;
import io.zicteam.zeconomy.menu.AtmMenu;
import io.zicteam.zeconomy.network.ZEconomyNetwork;
import io.zicteam.zeconomy.util.InventoryUtils;

public class AtmScreen extends AbstractContainerScreen<AtmMenu> {
    private EditBox amountBox;
    private GuiLayoutConfig.AtmLayout layout;
    private GuiLayoutConfig.GuiTheme theme;

    public AtmScreen(AtmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("screen.zeconomy.atm.title"));
        this.layout = GuiLayoutConfig.atm();
        this.theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
    }

    @Override
    protected void init() {
        GuiLayoutConfig.reload();
        layout = GuiLayoutConfig.atm();
        theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
        super.init();
        int left = leftPos;
        int top = topPos;

        amountBox = new EditBox(font, left + layout.amountX, top + layout.amountY, layout.amountW, layout.amountH, Component.translatable("screen.zeconomy.common.amount"));
        amountBox.setValue("100");
        amountBox.setMaxLength(16);
        addRenderableWidget(amountBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.atm.deposit_z"), b -> send("ATM_DEPOSIT_Z")).bounds(left + layout.resolvedDepositZX(), top + layout.resolvedDepositZY(), layout.buttonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.atm.withdraw_z"), b -> send("ATM_WITHDRAW_Z")).bounds(left + layout.resolvedWithdrawZX(), top + layout.resolvedWithdrawZY(), layout.buttonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.atm.deposit_b"), b -> send("ATM_DEPOSIT_B")).bounds(left + layout.resolvedDepositBX(), top + layout.resolvedDepositBY(), layout.buttonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.atm.deposit_all_z"), b -> send("ATM_DEPOSIT_ALL_Z")).bounds(left + layout.resolvedDepositAllZX(), top + layout.resolvedDepositAllZY(), layout.buttonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.atm.deposit_all_b"), b -> send("ATM_DEPOSIT_ALL_B")).bounds(left + layout.resolvedDepositAllBX(), top + layout.resolvedDepositAllBY(), layout.buttonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.common.refresh"), b -> send("SYNC")).bounds(left + layout.resolvedRefreshX(), top + layout.resolvedRefreshY(), layout.smallButtonW, layout.buttonH).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.common.close"), b -> onClose()).bounds(left + layout.resolvedCloseX(), top + layout.resolvedCloseY(), layout.smallButtonW, layout.buttonH).build());
    }

    private void send(String action) {
        ZEconomyNetwork.sendBankAction(action, "", parseAmount());
    }

    private double parseAmount() {
        try {
            return Math.max(0.0D, Double.parseDouble(amountBox.getValue()));
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        int bgStart = GuiLayoutConfig.GuiTheme.parseColor(theme.backgroundStart, 0xE61E222A);
        int bgEnd = GuiLayoutConfig.GuiTheme.parseColor(theme.backgroundEnd, 0xE612151C);
        int header = GuiLayoutConfig.GuiTheme.parseColor(theme.headerPanel, 0xFF2F3A47);
        int title = GuiLayoutConfig.GuiTheme.parseColor(theme.titleColor, 0xEAF2FF);
        int label = GuiLayoutConfig.GuiTheme.parseColor(theme.labelColor, 0xACB8CA);
        int walletLeft = GuiLayoutConfig.GuiTheme.parseColor(theme.atmWalletLeftColor, 0xB6D3FF);
        int walletRight = GuiLayoutConfig.GuiTheme.parseColor(theme.atmWalletRightColor, 0xF4D37D);
        int invLeft = GuiLayoutConfig.GuiTheme.parseColor(theme.atmInvLeftColor, 0xA9D9F5);
        int invRight = GuiLayoutConfig.GuiTheme.parseColor(theme.atmInvRightColor, 0xF2D69D);
        graphics.fillGradient(left, top, left + imageWidth, top + imageHeight, bgStart, bgEnd);
        graphics.fill(left + 6, top + 6, left + imageWidth - 6, top + 22, header);
        graphics.drawString(font, Component.translatable("screen.zeconomy.atm.header"), left + 12, top + 10, title, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.common.amount"), left + layout.amountX, top + layout.amountY - 10, label, false);

        double sdm = CurrencyPlayerData.CLIENT.getBalance("z_coin");
        double b = CurrencyPlayerData.CLIENT.getBalance("b_coin");
        int sdmItems = 0;
        int bItems = 0;
        if (Minecraft.getInstance().player != null) {
            sdmItems = InventoryUtils.countItem(Minecraft.getInstance().player, EconomyContent.Z_COIN_ITEM.get());
            bItems = InventoryUtils.countItem(Minecraft.getInstance().player, EconomyContent.B_COIN_ITEM.get());
        }
        graphics.drawString(font, Component.translatable("screen.zeconomy.atm.wallet_z", String.format("%.2f", sdm)), left + layout.resolvedDepositZX(), top + layout.walletY, walletLeft, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.atm.wallet_b", String.format("%.2f", b)), left + layout.resolvedWithdrawZX(), top + layout.walletY, walletRight, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.atm.inv_z", sdmItems), left + layout.resolvedDepositZX(), top + layout.invY, invLeft, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.atm.inv_b", bItems), left + layout.resolvedWithdrawZX(), top + layout.invY, invRight, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }
}
