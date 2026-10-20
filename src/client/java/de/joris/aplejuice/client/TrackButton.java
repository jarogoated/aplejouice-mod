package de.joris.aplejuice.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

public final class TrackButton extends AbstractWidget {
    public enum Kind { BUTTON, NAV, SWITCH, FLAG, RESET }
    private final Runnable action;
    private final boolean selected;
    private Kind kind = Kind.BUTTON;
    private String icon;
    public TrackButton(int x, int y, int w, int h, String label, boolean selected, Runnable action) {
        super(x, y, w, h, Component.literal(label)); this.action = action; this.selected = selected;
    }
    public TrackButton style(Kind kind, String icon) { this.kind = kind; this.icon = icon; return this; }
    protected void renderWidget(GuiGraphics g, int mx, int my, float delta) {
        var font = Minecraft.getInstance().font;
        int x = getX(), y = getY(); boolean hover = isHoveredOrFocused();
        if (kind == Kind.SWITCH) {
            int color = selected ? 0xFFFFBA4F : 0xFFB7A394;
            text(g, getMessage(), x, y + 5, color, 1.5F);
            int sx = getRight() - 32;
            g.fill(sx, y + 4, getRight(), y + 16, selected ? 0xFF995127 : 0xFF1C1916);
            g.renderOutline(sx, y + 4, 32, 12, selected ? 0xFFE79238 : 0xFF534335);
            g.fill(selected ? getRight() - 11 : sx + 2, y + 5, selected ? getRight() - 2 : sx + 11, y + 15, 0xFFFFE8CA);
            g.fill(selected ? sx + 5 : getRight() - 9, y + 7, selected ? sx + 6 : getRight() - 7, y + 13, 0xFFBA9061);
            if (hover) g.renderOutline(sx - 1, y + 3, 34, 14, 0xFFFFB84D);
            return;
        }
        if (kind == Kind.NAV) {
            GuiAssets.rounded(g, x, y, width, height, 3, selected ? 0xFFFF9B38 : 0xFF2C2824, 0xFF2C2824);
            if (selected) {
                GuiAssets.rounded(g, x + 1, y + 1, width - 2, height - 2, 2, 0xFFDC6232, 0xFF2C2824);
            }
            if (icon != null) GuiAssets.draw(g, icon, x + 5, y + 5, 18, 18);
            else {
                g.renderOutline(x + 5, y + 5, 18, 18, 0xFF6D513B);
                g.drawCenteredString(font, this.getY() < 110 ? "◇" : "♙", x + 14, y + 10, 0xFFD9B88F);
            }
            Component text = selected ? getMessage().copy().withStyle(ChatFormatting.BOLD) : getMessage();
            text(g, text, x + 29, y + 8, 0xFFFFEDDA, 1.5F);
            return;
        }
        if (kind == Kind.FLAG) {
            GuiAssets.rounded(g, x, y, width, height, 3, selected ? 0xFFFF9B38 : hover ? 0xFFBA8852 : 0xFF2C2824, 0xFF2C2824);
            GuiAssets.draw(g, icon, x + 3, y + 3, 24, 18);
            g.drawString(font, getMessage(), x + 31, y + 8, 0xFFFFEDDA, false);
            return;
        }
        if (kind == Kind.RESET) {
            if (hover) g.fill(x, y, getRight(), getBottom(), 0xFF49372B);
            GuiAssets.draw(g, "reset", x + 13, y + 8, 17, 17);
            text(g, getMessage(), x + 34, y + 10, 0xFFFFEDDA, 1.5F);
            return;
        }
        GuiAssets.rounded(g, x, y, width, height, 3, hover || selected ? 0xFFFFA23D : 0xFF805B3E, hover ? 0xFF503A2C : 0xFF342D26);
        g.drawCenteredString(font, getMessage(), x + width / 2, y + (height - 8) / 2, active ? 0xFFFFEDDA : 0xFF938274);
    }
    private void text(GuiGraphics g, Component text, int x, int y, int color, float size) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(size, size);
        g.drawString(Minecraft.getInstance().font, text, 0, 0, color, false); g.pose().popMatrix();
    }
    public void onClick(MouseButtonEvent event, boolean doubleClick) { action.run(); }
    public boolean keyPressed(KeyEvent event) {
        if (isFocused() && (event.key() == 257 || event.key() == 32)) { action.run(); return true; }
        return super.keyPressed(event);
    }
    protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
}
