package io.zicteam.zeconomy.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import io.zicteam.zeconomy.network.ZEconomyNetwork;

public class GuiLayoutEditorScreen extends Screen {
    private final String target;
    private final CompoundTag values;
    private final List<Element> elements = new ArrayList<>();
    private Element selected;
    private Element dragging;
    private int dragDx;
    private int dragDy;
    private int previewLeft;
    private int previewTop;
    private int inspectorX;
    private EditBox hintBox;

    public GuiLayoutEditorScreen(String target, CompoundTag values) {
        super(Component.literal("GUI Editor"));
        this.target = target;
        this.values = values.copy();
    }

    @Override
    protected void init() {
        inspectorX = width - 220;
        int x = inspectorX + 10;
        int y = 14;
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            ZEconomyNetwork.sendLayoutEditorSave(target, values);
            onClose();
        }).bounds(x, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(x + 104, y, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Table ON/OFF"), b -> {
            if ("exchange".equalsIgnoreCase(target)) {
                values.putBoolean("table.enabled", !tableEnabled());
                rebuildElements();
            }
        }).bounds(x, y + 24, 200, 20).build()).visible = "exchange".equalsIgnoreCase(target);
        addRenderableWidget(Button.builder(Component.literal("X-1"), b -> nudge(-1, 0)).bounds(x, y + 78, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("X+1"), b -> nudge(1, 0)).bounds(x + 52, y + 78, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Y-1"), b -> nudge(0, -1)).bounds(x + 104, y + 78, 48, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Y+1"), b -> nudge(0, 1)).bounds(x + 156, y + 78, 44, 18).build());

        hintBox = new EditBox(font, x, y + 52, 200, 18, Component.literal("hint"));
        hintBox.setEditable(false);
        hintBox.setCanLoseFocus(false);
        hintBox.setValue("LMB: click and drag");
        addRenderableWidget(hintBox);
        rebuildElements();
    }

    private void nudge(int dx, int dy) {
        if (selected == null) return;
        applyX(selected, selected.x + dx);
        applyY(selected, selected.y + dy);
        rebuildElements();
        updateHint();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        for (int i = elements.size() - 1; i >= 0; i--) {
            Element e = elements.get(i);
            if (e.contains((int) mouseX, (int) mouseY)) {
                selected = e;
                dragging = e;
                dragDx = (int) mouseX - e.x;
                dragDy = (int) mouseY - e.y;
                updateHint();
                return true;
            }
        }
        selected = null;
        dragging = null;
        updateHint();
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && dragging != null) {
            int nx = Mth.clamp((int) mouseX - dragDx, previewLeft, previewLeft + imageW() - dragging.w);
            int ny = Mth.clamp((int) mouseY - dragDy, previewTop, previewTop + imageH() - dragging.h);
            applyX(dragging, nx);
            applyY(dragging, ny);
            rebuildElements();
            updateHint();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        drawPreview(g);
        drawElements(g, mouseX, mouseY);
        drawInspector(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void rebuildElements() {
        elements.clear();
        previewLeft = Math.max(8, (inspectorX - imageW()) / 2);
        previewTop = Math.max(10, (height - imageH()) / 2);
        if ("atm".equalsIgnoreCase(target)) {
            elements.add(new Element("amount field", previewLeft + v("amountX", 16), previewTop + v("amountY", 34), v("amountW", 90), v("amountH", 18), "amountX", "amountY", null, null));
            elements.add(new Element("deposit z", previewLeft + v("depositZX", 16), previewTop + v("depositZY", 58), v("buttonW", 100), v("buttonH", 18), "depositZX", "depositZY", null, null));
            elements.add(new Element("withdraw z", previewLeft + v("withdrawZX", 124), previewTop + v("withdrawZY", 58), v("buttonW", 100), v("buttonH", 18), "withdrawZX", "withdrawZY", null, null));
            elements.add(new Element("deposit b", previewLeft + v("depositBX", 16), previewTop + v("depositBY", 80), v("buttonW", 100), v("buttonH", 18), "depositBX", "depositBY", null, null));
            elements.add(new Element("deposit all z", previewLeft + v("depositAllZX", 16), previewTop + v("depositAllZY", 102), v("buttonW", 100), v("buttonH", 18), "depositAllZX", "depositAllZY", null, null));
            elements.add(new Element("deposit all b", previewLeft + v("depositAllBX", 124), previewTop + v("depositAllBY", 102), v("buttonW", 100), v("buttonH", 18), "depositAllBX", "depositAllBY", null, null));
            elements.add(new Element("refresh", previewLeft + v("refreshX", 16), previewTop + v("refreshY", 124), v("smallButtonW", 70), v("buttonH", 18), "refreshX", "refreshY", null, null));
            elements.add(new Element("close", previewLeft + v("closeX", 154), previewTop + v("closeY", 124), v("smallButtonW", 70), v("buttonH", 18), "closeX", "closeY", null, null));
            return;
        }
        if ("bank".equalsIgnoreCase(target)) {
            elements.add(new Element("amount field", previewLeft + v("amountX", 16), previewTop + v("amountY", 34), v("amountW", 90), v("amountH", 18), "amountX", "amountY", null, null));
            elements.add(new Element("currency sdm", previewLeft + v("currencyZX", 112), previewTop + v("currencyZY", 34), v("currencyZW", 70), 18, "currencyZX", "currencyZY", null, null));
            elements.add(new Element("currency b", previewLeft + v("currencyBX", 184), previewTop + v("currencyBY", 34), v("currencyBW", 60), 18, "currencyBX", "currencyBY", null, null));
            elements.add(new Element("deposit", previewLeft + v("depositX", 16), previewTop + v("depositY", 60), 72, 18, "depositX", "depositY", null, null));
            elements.add(new Element("withdraw", previewLeft + v("withdrawX", 92), previewTop + v("withdrawY", 60), 72, 18, "withdrawX", "withdrawY", null, null));
            elements.add(new Element("exchange", previewLeft + v("exchangeX", 168), previewTop + v("exchangeY", 60), 76, 18, "exchangeX", "exchangeY", null, null));
            elements.add(new Element("claim mail", previewLeft + v("claimMailX", 16), previewTop + v("claimMailY", 84), 100, 18, "claimMailX", "claimMailY", null, null));
            elements.add(new Element("refresh", previewLeft + v("refreshX", 120), previewTop + v("refreshY", 84), 70, 18, "refreshX", "refreshY", null, null));
            elements.add(new Element("close", previewLeft + v("closeX", 194), previewTop + v("closeY", 84), 50, 18, "closeX", "closeY", null, null));
            return;
        }
        if ("mailbox".equalsIgnoreCase(target)) {
            elements.add(new Element("recipient", previewLeft + v("recipientX", 8), previewTop + v("recipientY", 20), v("recipientW", 104), v("recipientH", 16), "recipientX", "recipientY", null, null));
            elements.add(new Element("send button", previewLeft + v("sendButtonX", 116), previewTop + v("sendButtonY", 20), v("buttonW", 52), v("buttonH", 18), "sendButtonX", "sendButtonY", null, null));
            elements.add(new Element("claim button", previewLeft + v("claimButtonX", 116), previewTop + v("claimButtonY", 40), v("buttonW", 52), v("buttonH", 18), "claimButtonX", "claimButtonY", null, null));
            return;
        }

        int tx = previewLeft + tradeX();
        elements.add(new Element("input slot", previewLeft + templateX(), previewTop + inputY(), 18, 18, "templateSlotX", "inputSlotY", "table.templateSlotCol", "table.inputSlotRow"));
        elements.add(new Element("output slot", previewLeft + templateX(), previewTop + outputY(), 18, 18, "templateSlotX", "outputSlotY", "table.templateSlotCol", "table.outputSlotRow"));
        elements.add(new Element("text accepts label", previewLeft + textX(), previewTop + acceptsLabelY(), 118, 10, "textX", "acceptsLabelY", "table.textCol", "table.acceptsLabelRow"));
        elements.add(new Element("text accepts value", previewLeft + textX(), previewTop + acceptsValueY(), 118, 10, "textX", "acceptsValueY", "table.textCol", "table.acceptsValueRow"));
        elements.add(new Element("text gives label", previewLeft + textX(), previewTop + givesLabelY(), 118, 10, "textX", "givesLabelY", "table.textCol", "table.givesLabelRow"));
        elements.add(new Element("text gives value", previewLeft + textX(), previewTop + givesValueY(), 118, 10, "textX", "givesValueY", "table.textCol", "table.givesValueRow"));
        elements.add(new Element("storage label", previewLeft + storageX(), previewTop + storageLabelY(), 120, 10, "storageX", "storageLabelY", "table.storageCol", "table.storageLabelRow"));
        elements.add(new Element("storage grid", previewLeft + storageX(), previewTop + storageY(), 162, 54, "storageX", "storageY", "table.storageCol", "table.storageRow"));
        elements.add(new Element("trade button", tx, previewTop + tradeY(), v("tradeButtonW", 54), v("tradeButtonH", 20), "tradeButtonX", "tradeButtonY", null, "table.tradeButtonRow"));
        elements.add(new Element("player inventory", previewLeft + playerInvX(), previewTop + v("playerInvY", 142), 162, 54, "playerInvX", "playerInvY", null, null));
        elements.add(new Element("hotbar", previewLeft + hotbarX(), previewTop + v("hotbarY", 200), 162, 18, "hotbarX", "hotbarY", null, null));
    }

    private void drawPreview(GuiGraphics g) {
        int l = previewLeft;
        int t = previewTop;
        g.fillGradient(l, t, l + imageW(), t + imageH(), 0xE61E222A, 0xE612151C);
        if ("atm".equalsIgnoreCase(target)) {
            g.drawString(font, "ATM", l + 10, t + 10, 0xEAF2FF, false);
            button(g, l + v("depositZX", 16), t + v("depositZY", 58), v("buttonW", 100), v("buttonH", 18), "Deposit Z");
            button(g, l + v("withdrawZX", 124), t + v("withdrawZY", 58), v("buttonW", 100), v("buttonH", 18), "Withdraw Z");
            button(g, l + v("depositBX", 16), t + v("depositBY", 80), v("buttonW", 100), v("buttonH", 18), "Deposit B");
            button(g, l + v("depositAllZX", 16), t + v("depositAllZY", 102), v("buttonW", 100), v("buttonH", 18), "Deposit all Z");
            button(g, l + v("depositAllBX", 124), t + v("depositAllBY", 102), v("buttonW", 100), v("buttonH", 18), "Deposit all B");
            button(g, l + v("refreshX", 16), t + v("refreshY", 124), v("smallButtonW", 70), v("buttonH", 18), "Refresh");
            button(g, l + v("closeX", 154), t + v("closeY", 124), v("smallButtonW", 70), v("buttonH", 18), "Close");
            return;
        }
        if ("bank".equalsIgnoreCase(target)) {
            g.drawString(font, "Bank", l + v("headerX", 12), t + v("headerY", 14), 0xEAF2FF, false);
            button(g, l + v("currencyZX", 112), t + v("currencyZY", 34), v("currencyZW", 70), 18, "Val Z");
            button(g, l + v("currencyBX", 184), t + v("currencyBY", 34), v("currencyBW", 60), 18, "Val B");
            button(g, l + v("depositX", 16), t + v("depositY", 60), 72, 18, "Bank +");
            button(g, l + v("withdrawX", 92), t + v("withdrawY", 60), 72, 18, "Bank -");
            button(g, l + v("exchangeX", 168), t + v("exchangeY", 60), 76, 18, "Exchange");
            button(g, l + v("claimMailX", 16), t + v("claimMailY", 84), 100, 18, "Claim Mail");
            button(g, l + v("refreshX", 120), t + v("refreshY", 84), 70, 18, "Refresh");
            button(g, l + v("closeX", 194), t + v("closeY", 84), 50, 18, "Close");
            return;
        }
        if ("mailbox".equalsIgnoreCase(target)) {
            g.drawString(font, "Mailbox", l + 8, t + 8, 0xEAF2FF, false);
            button(g, l + v("sendButtonX", 116), t + v("sendButtonY", 20), v("buttonW", 52), v("buttonH", 18), "Send");
            button(g, l + v("claimButtonX", 116), t + v("claimButtonY", 40), v("buttonW", 52), v("buttonH", 18), "Claim");
            return;
        }
        g.drawString(font, "Offer Setup", l + 8, t + 8, 0xEAF2FF, false);
        g.drawString(font, "Accepts", l + textX(), t + acceptsLabelY(), 0xC8D2E0, false);
        g.drawString(font, "-", l + textX(), t + acceptsValueY(), 0xD6DFED, false);
        g.drawString(font, "Gives", l + textX(), t + givesLabelY(), 0xC8D2E0, false);
        g.drawString(font, "-", l + textX(), t + givesValueY(), 0xD6DFED, false);
        g.drawString(font, "Storage", l + storageX(), t + storageLabelY(), 0xC8D2E0, false);
        slot(g, l + templateX(), t + inputY());
        slot(g, l + templateX(), t + outputY());
        drawSlots(g, l + storageX(), t + storageY(), 9, 3);
        drawSlots(g, l + playerInvX(), t + v("playerInvY", 142), 9, 3);
        drawSlots(g, l + hotbarX(), t + v("hotbarY", 200), 9, 1);
        button(g, l + tradeX(), t + tradeY(), v("tradeButtonW", 54), v("tradeButtonH", 20), "Trade");

        if (tableEnabled()) {
            int ox = v("table.originX", 8);
            int oy = v("table.originY", 8);
            int cs = Math.max(1, v("table.colStep", 18));
            int rs = Math.max(1, v("table.rowStep", 14));
            for (int gx = ox; gx < imageW(); gx += cs) g.fill(l + gx, t, l + gx + 1, t + imageH(), 0x22395E84);
            for (int gy = oy; gy < imageH(); gy += rs) g.fill(l, t + gy, l + imageW(), t + gy + 1, 0x22395E84);
        }
    }

    private void drawInspector(GuiGraphics g) {
        g.fill(inspectorX, 8, width - 8, height - 8, 0xCC0F1520);
        g.drawString(font, "GUI Editor: " + target, inspectorX + 8, 16, 0xEAF2FF, false);
        g.drawString(font, "Drag any highlighted element.", inspectorX + 8, 124, 0xB5C4D7, false);
        if ("exchange".equalsIgnoreCase(target)) g.drawString(font, "Table: " + (tableEnabled() ? "ON" : "OFF"), inspectorX + 8, 138, 0xD6DFED, false);
        if (selected != null) g.drawString(font, "Selected: " + selected.name, inspectorX + 8, 152, 0xD6DFED, false);
    }

    private void drawElements(GuiGraphics g, int mx, int my) {
        for (Element e : elements) {
            boolean hover = e.contains(mx, my);
            boolean active = selected == e;
            int fill = active ? 0x88FFD166 : (hover ? 0x668DB7EA : 0x334B6180);
            int border = active ? 0xFFFFE09A : 0xFF8FB2D8;
            g.fill(e.x, e.y, e.x + e.w, e.y + e.h, fill);
            g.fill(e.x, e.y, e.x + e.w, e.y + 1, border);
            g.fill(e.x, e.y, e.x + 1, e.y + e.h, border);
            g.fill(e.x + e.w - 1, e.y, e.x + e.w, e.y + e.h, border);
            g.fill(e.x, e.y + e.h - 1, e.x + e.w, e.y + e.h, border);
        }
    }

    private void applyX(Element e, int absX) {
        if (e.xKey == null && e.tableXKey == null) return;
        int local = absX - previewLeft;
        if ("exchange".equalsIgnoreCase(target) && tableEnabled() && e.tableXKey != null) {
            int col = Math.round((local - v("table.originX", 8)) / (float) Math.max(1, v("table.colStep", 18)));
            values.putInt(e.tableXKey, col);
        } else if (e.xKey != null) values.putInt(e.xKey, local);
    }

    private void applyY(Element e, int absY) {
        if (e.yKey == null && e.tableYKey == null) return;
        int local = absY - previewTop;
        if ("exchange".equalsIgnoreCase(target) && tableEnabled() && e.tableYKey != null) {
            int row = Math.round((local - v("table.originY", 8)) / (float) Math.max(1, v("table.rowStep", 14)));
            values.putInt(e.tableYKey, row);
        } else if (e.yKey != null) values.putInt(e.yKey, local);
    }

    private void updateHint() {
        if (hintBox == null) return;
        if (selected == null) hintBox.setValue("LMB: click and drag");
        else hintBox.setValue(selected.name + " | X=" + (selected.x - previewLeft) + " Y=" + (selected.y - previewTop));
    }

    private boolean tableEnabled() { return !values.contains("table.enabled") || values.getBoolean("table.enabled"); }
    private int imageW() {
        if ("atm".equalsIgnoreCase(target)) return v("imageWidth", 240);
        if ("bank".equalsIgnoreCase(target)) return v("imageWidth", 260);
        if ("mailbox".equalsIgnoreCase(target)) return v("imageWidth", 176);
        return v("imageWidth", 230);
    }
    private int imageH() {
        if ("atm".equalsIgnoreCase(target)) return v("imageHeight", 178);
        if ("bank".equalsIgnoreCase(target)) return v("imageHeight", 180);
        if ("mailbox".equalsIgnoreCase(target)) return v("imageHeight", 206);
        return v("imageHeight", 228);
    }
    private int templateX() { return tableEnabled() ? tableX(v("table.templateSlotCol", 0)) : v("templateSlotX", 18); }
    private int inputY() { return tableEnabled() ? tableY(v("table.inputSlotRow", 2)) : v("inputSlotY", 34); }
    private int outputY() { return tableEnabled() ? tableY(v("table.outputSlotRow", 4)) : v("outputSlotY", 64); }
    private int textX() { return tableEnabled() ? tableX(v("table.textCol", 2)) : v("textX", 44); }
    private int acceptsLabelY() { return tableEnabled() ? tableY(v("table.acceptsLabelRow", 1)) : v("acceptsLabelY", 22); }
    private int acceptsValueY() { return tableEnabled() ? tableY(v("table.acceptsValueRow", 2)) : v("acceptsValueY", 36); }
    private int givesLabelY() { return tableEnabled() ? tableY(v("table.givesLabelRow", 3)) : v("givesLabelY", 52); }
    private int givesValueY() { return tableEnabled() ? tableY(v("table.givesValueRow", 4)) : v("givesValueY", 66); }
    private int storageX() { return tableEnabled() ? tableX(v("table.storageCol", 2)) : v("storageX", 44); }
    private int storageLabelY() { return tableEnabled() ? tableY(v("table.storageLabelRow", 5)) : v("storageLabelY", 74); }
    private int storageY() { return tableEnabled() ? tableY(v("table.storageRow", 6)) : v("storageY", 84); }
    private int playerInvX() { return v("playerInvX", 8); }
    private int hotbarX() { return v("hotbarX", 8); }
    private int tradeX() { return v("tradeButtonX", imageW() - v("tradeButtonW", 54) - 8); }
    private int tradeY() { return tableEnabled() ? tableY(v("table.tradeButtonRow", 3)) : v("tradeButtonY", 49); }
    private int tableX(int c) { return v("table.originX", 8) + c * Math.max(1, v("table.colStep", 18)); }
    private int tableY(int r) { return v("table.originY", 8) + r * Math.max(1, v("table.rowStep", 14)); }
    private int v(String k, int d) { return values.contains(k) ? values.getInt(k) : d; }

    private void slot(GuiGraphics g, int x, int y) { g.fill(x, y, x + 18, y + 18, 0xAA0E131B); g.fill(x + 1, y + 1, x + 17, y + 17, 0x661E2A39); }
    private void drawSlots(GuiGraphics g, int x, int y, int cols, int rows) { for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) slot(g, x + c * 18, y + r * 18); }
    private void button(GuiGraphics g, int x, int y, int w, int h, String text) { g.fill(x, y, x + w, y + h, 0xFF777777); g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5F5F5F); g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, 0xFFFFFF); }

    private static final class Element {
        private final String name;
        private final int x, y, w, h;
        private final String xKey, yKey, tableXKey, tableYKey;
        private Element(String name, int x, int y, int w, int h, String xKey, String yKey, String tableXKey, String tableYKey) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.xKey = xKey;
            this.yKey = yKey;
            this.tableXKey = tableXKey;
            this.tableYKey = tableYKey;
        }
        private boolean contains(int mx, int my) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    }
}
