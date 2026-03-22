package net.sixik.zeconomy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.sixik.zeconomy.config.GuiLayoutConfig;
import net.sixik.zeconomy.menu.ExchangeMenu;
import net.sixik.zeconomy.network.ZEconomyNetwork;

public class ExchangeScreen extends AbstractContainerScreen<ExchangeMenu> {
    private GuiLayoutConfig.ExchangeLayout layout;
    private GuiLayoutConfig.GuiTheme theme;

    public ExchangeScreen(ExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("screen.zeconomy.exchange.title"));
        this.layout = GuiLayoutConfig.exchange();
        this.theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
    }

    @Override
    protected void init() {
        GuiLayoutConfig.reload();
        layout = GuiLayoutConfig.exchange();
        theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
        super.init();
        int left = leftPos;
        int top = topPos;
        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.exchange.trade"), b ->
            ZEconomyNetwork.sendExchangeTrade(menu.getBlockPos())
        ).bounds(left + layout.resolvedTradeButtonX(), top + layout.resolvedTradeButtonY(), layout.tradeButtonW, layout.tradeButtonH).build());
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
        int section = GuiLayoutConfig.GuiTheme.parseColor(theme.sectionPanel, 0x662B313A);
        int sectionAlt = GuiLayoutConfig.GuiTheme.parseColor(theme.sectionPanelAlt, 0x44101725);
        int title = GuiLayoutConfig.GuiTheme.parseColor(theme.titleColor, 0xEAF2FF);
        int label = GuiLayoutConfig.GuiTheme.parseColor(theme.labelColor, 0xC8D2E0);

        graphics.fillGradient(left, top, left + imageWidth, top + imageHeight, bgStart, bgEnd);
        graphics.fill(left + 6, top + 16, left + imageWidth - 6, top + 74, section);
        graphics.fill(left + 6, top + 78, left + imageWidth - 6, top + 138, section);
        graphics.fill(left + 6, top + 138, left + imageWidth - 6, top + imageHeight - 6, sectionAlt);

        graphics.drawString(font, Component.translatable("screen.zeconomy.exchange.header"), left + 8, top + 8, title, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.exchange.accepts"), left + layout.resolvedTextX(), top + layout.resolvedAcceptsLabelY(), label, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.exchange.gives"), left + layout.resolvedTextX(), top + layout.resolvedGivesLabelY(), label, false);
        graphics.drawString(font, Component.translatable("screen.zeconomy.exchange.storage"), left + layout.resolvedStorageX(), top + layout.resolvedStorageLabelY(), label, false);

        drawSlotFrame(graphics, left + layout.resolvedTemplateSlotX(), top + layout.resolvedInputSlotY());
        drawSlotFrame(graphics, left + layout.resolvedTemplateSlotX(), top + layout.resolvedOutputSlotY());
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, left + layout.resolvedStorageX() + col * 18, top + layout.resolvedStorageY() + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, left + layout.playerInvX + col * 18, top + layout.playerInvY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, left + layout.hotbarX + col * 18, top + layout.hotbarY);
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y) {
        int outer = GuiLayoutConfig.GuiTheme.parseColor(theme.slotOuter, 0xAA0E131B);
        int inner = GuiLayoutConfig.GuiTheme.parseColor(theme.slotInner, 0x661E2A39);
        int light = GuiLayoutConfig.GuiTheme.parseColor(theme.slotLight, 0x99AAB6C7);
        int dark = GuiLayoutConfig.GuiTheme.parseColor(theme.slotDark, 0x8839455A);
        graphics.fill(x, y, x + 18, y + 18, outer);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, inner);
        graphics.fill(x + 1, y + 1, x + 17, y + 2, light);
        graphics.fill(x + 1, y + 1, x + 2, y + 17, light);
        graphics.fill(x + 16, y + 2, x + 17, y + 17, dark);
        graphics.fill(x + 2, y + 16, x + 17, y + 17, dark);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack in = menu.getInputTemplateStack();
        ItemStack out = menu.getOutputTemplateStack();
        String inText = in.isEmpty() ? "-" : trimToWidth(in.getHoverName().getString() + " x" + in.getCount(), layout.maxTextWidth);
        String outText = out.isEmpty() ? "-" : trimToWidth(out.getHoverName().getString() + " x" + out.getCount(), layout.maxTextWidth);
        int value = GuiLayoutConfig.GuiTheme.parseColor(theme.valueColor, 0xD6DFED);
        int muted = GuiLayoutConfig.GuiTheme.parseColor(theme.mutedColor, 0xAEB8C6);
        graphics.drawString(font, Component.literal(inText), layout.resolvedTextX(), layout.resolvedAcceptsValueY(), value, false);
        graphics.drawString(font, Component.literal(outText), layout.resolvedTextX(), layout.resolvedGivesValueY(), value, false);
        graphics.drawString(font, this.playerInventoryTitle, layout.playerInvX, layout.playerInvY - 10, muted, false);
    }

    private String trimToWidth(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String dots = "...";
        int dotsWidth = font.width(dots);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (font.width(sb.toString() + c) + dotsWidth > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb + dots;
    }
}
