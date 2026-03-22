package io.zicteam.zeconomy.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import io.zicteam.zeconomy.config.GuiLayoutConfig;
import io.zicteam.zeconomy.menu.MailboxMenu;
import io.zicteam.zeconomy.network.ZEconomyNetwork;

public class MailboxScreen extends AbstractContainerScreen<MailboxMenu> {
    private EditBox recipientBox;
    private GuiLayoutConfig.MailboxLayout layout;
    private GuiLayoutConfig.GuiTheme theme;

    public MailboxScreen(MailboxMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("screen.zeconomy.mailbox.title"));
        this.layout = GuiLayoutConfig.mailbox();
        this.theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
    }

    @Override
    protected void init() {
        GuiLayoutConfig.reload();
        layout = GuiLayoutConfig.mailbox();
        theme = GuiLayoutConfig.theme();
        this.imageWidth = layout.imageWidth;
        this.imageHeight = layout.imageHeight;
        super.init();
        int left = leftPos;
        int top = topPos;
        recipientBox = new EditBox(font, left + layout.recipientX, top + layout.recipientY, layout.recipientW, layout.recipientH, Component.translatable("screen.zeconomy.mailbox.recipient"));
        recipientBox.setHint(Component.translatable("screen.zeconomy.mailbox.hint_player"));
        recipientBox.setCanLoseFocus(false);
        setFocused(recipientBox);
        setInitialFocus(recipientBox);
        addRenderableWidget(recipientBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.mailbox.send"), b ->
            ZEconomyNetwork.sendMailboxAction("MAILBOX_SEND", menu.getBlockPos(), recipientBox.getValue())
        ).bounds(left + layout.sendButtonX, top + layout.sendButtonY, layout.buttonW, layout.buttonH).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.zeconomy.mailbox.claim"), b ->
            ZEconomyNetwork.sendMailboxAction("MAILBOX_CLAIM", menu.getBlockPos(), "")
        ).bounds(left + layout.claimButtonX, top + layout.claimButtonY, layout.buttonW, layout.buttonH).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (recipientBox != null && recipientBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (recipientBox != null && recipientBox.isFocused() && this.minecraft != null
            && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (recipientBox != null && recipientBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
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
        graphics.fillGradient(left, top, left + imageWidth, top + imageHeight, bgStart, bgEnd);
        graphics.fill(left + 6, top + 16, left + imageWidth - 6, top + 104, section);
        graphics.fill(left + 6, top + 104, left + imageWidth - 6, top + imageHeight - 6, sectionAlt);
        graphics.drawString(font, Component.translatable("screen.zeconomy.mailbox.header"), left + 8, top + 8, title, false);

        // 3x3 send area slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlotFrame(graphics, left + 62 + col * 18, top + 50 + row * 18);
            }
        }
        // Player inventory slots (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(graphics, left + 8 + col * 18, top + 116 + row * 18);
            }
        }
        // Hotbar slots
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(graphics, left + 8 + col * 18, top + 174);
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
        int label = GuiLayoutConfig.GuiTheme.parseColor(theme.labelColor, 0xC9D7E8);
        int muted = GuiLayoutConfig.GuiTheme.parseColor(theme.mutedColor, 0xAEB8C6);
        graphics.drawString(font, Component.translatable("screen.zeconomy.mailbox.send_slots"), 8, 40, label, false);
        graphics.drawString(font, this.playerInventoryTitle, 8, 104, muted, false);
    }
}
