package net.sixik.zeconomy.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.sixik.zeconomy.CustomPlayerData;
import net.sixik.zeconomy.config.GuiLayoutConfig;
import net.sixik.zeconomy.currencies.data.CurrencyPlayerData;
import net.sixik.zeconomy.menu.BankMenu;
import net.sixik.zeconomy.network.ZEconomyNetwork;

public class BankScreen extends AbstractContainerScreen<BankMenu> {
    private EditBox amountBox;
    private String selectedCurrency = "z_coin";
    private GuiLayoutConfig.BankLayout layout;
    private GuiLayoutConfig.GuiTheme theme;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("screen.zeconomy.bank.title"));
        this.layout = GuiLayoutConfig.bank();
        this.theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
    }

    @Override
    protected void init() {
        GuiLayoutConfig.reload();
        layout = GuiLayoutConfig.bank();
        theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
        super.init();
        int left = this.leftPos;
        int top = this.topPos;

        this.amountBox = new EditBox(this.font, left + layout.amountX, top + layout.amountY, layout.amountW, layout.amountH, Component.translatable("screen.zeconomy.common.amount"));
        this.amountBox.setValue("100");
        this.amountBox.setMaxLength(16);
        this.addRenderableWidget(this.amountBox);

        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.currency_z"), b -> {
            selectedCurrency = "z_coin";
            b.setMessage(Component.translatable("screen.zeconomy.bank.currency_z"));
        }).bounds(left + layout.currencyZX, top + layout.currencyZY, layout.currencyZW, 18).build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.currency_b"), b -> {
            selectedCurrency = "b_coin";
            b.setMessage(Component.translatable("screen.zeconomy.bank.currency_b"));
        }).bounds(left + layout.currencyBX, top + layout.currencyBY, layout.currencyBW, 18).build());

        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.deposit"), b -> sendAction("BANK_DEPOSIT")).bounds(left + layout.depositX, top + layout.depositY, 72, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.withdraw"), b -> sendAction("BANK_WITHDRAW")).bounds(left + layout.withdrawX, top + layout.withdrawY, 72, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.exchange"), b -> sendAction("EXCHANGE_B_TO_Z")).bounds(left + layout.exchangeX, top + layout.exchangeY, 76, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.bank.claim_mail"), b -> sendAction("MAIL_CLAIM")).bounds(left + layout.claimMailX, top + layout.claimMailY, 100, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.common.refresh"), b -> sendAction("SYNC")).bounds(left + layout.refreshX, top + layout.refreshY, 70, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.common.close"), b -> this.onClose()).bounds(left + layout.closeX, top + layout.closeY, 50, 18).build());
    }

    private void sendAction(String action) {
        double amount = parseAmount();
        ZEconomyNetwork.sendBankAction(action, selectedCurrency, amount);
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
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        int bgStart = GuiLayoutConfig.GuiTheme.parseColor(theme.backgroundStart, 0xE6191B22);
        int bgEnd = GuiLayoutConfig.GuiTheme.parseColor(theme.backgroundEnd, 0xE6111218);
        int header = GuiLayoutConfig.GuiTheme.parseColor(theme.headerPanel, 0xFF2D3642);
        int sectionAlt = GuiLayoutConfig.GuiTheme.parseColor(theme.sectionPanelAlt, 0xAA101318);
        int title = GuiLayoutConfig.GuiTheme.parseColor(theme.titleColor, 0xE8EEF7);
        int walletLeft = GuiLayoutConfig.GuiTheme.parseColor(theme.atmWalletLeftColor, 0xB6D3FF);
        int walletRight = GuiLayoutConfig.GuiTheme.parseColor(theme.atmWalletRightColor, 0xF4D37D);
        int muted = GuiLayoutConfig.GuiTheme.parseColor(theme.mutedColor, 0xD0D4DB);
        int good = GuiLayoutConfig.GuiTheme.parseColor(theme.labelColor, 0x9CE4AF);
        int label = GuiLayoutConfig.GuiTheme.parseColor(theme.labelColor, 0xAEB8C6);

        graphics.fillGradient(left, top, left + this.imageWidth, top + this.imageHeight, bgStart, bgEnd);
        graphics.fill(left + 6, top + 6, left + this.imageWidth - 6, top + 28, header);
        graphics.fill(left + 8, top + 110, left + this.imageWidth - 8, top + this.imageHeight - 10, sectionAlt);

        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.header"), left + layout.headerX, top + layout.headerY, title, false);

        double sdm = CurrencyPlayerData.CLIENT.getBalance("z_coin");
        double b = CurrencyPlayerData.CLIENT.getBalance("b_coin");
        double bankZ = CustomPlayerData.CLIENT.data.nbt.getDouble("bank_z_coin");
        double bankB = CustomPlayerData.CLIENT.data.nbt.getDouble("bank_b_coin");
        int mailPending = CustomPlayerData.CLIENT.data.nbt.getInt("mail_pending");

        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.wallet_z", String.format("%.2f", sdm)), left + layout.walletZX, top + layout.walletZY, walletLeft, false);
        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.wallet_b", String.format("%.2f", b)), left + layout.walletBX, top + layout.walletBY, walletRight, false);
        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.bank_z", String.format("%.2f", bankZ)).withStyle(ChatFormatting.GRAY), left + layout.bankZX, top + layout.bankZY, muted, false);
        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.bank_b", String.format("%.2f", bankB)).withStyle(ChatFormatting.GRAY), left + layout.bankBX, top + layout.bankBY, muted, false);
        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.mail", mailPending), left + layout.mailX, top + layout.mailY, good, false);

        graphics.drawString(this.font, Component.translatable("screen.zeconomy.common.amount"), left + layout.amountLabelX, top + layout.amountLabelY, label, false);
        graphics.drawString(this.font, Component.translatable("screen.zeconomy.bank.selected", selectedCurrency), left + layout.selectedX, top + layout.selectedY, label, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Custom labels are rendered in renderBg.
    }
}
