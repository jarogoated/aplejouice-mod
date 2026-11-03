package de.joris.aplejuice.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** One quad per sprite prevents seams between independently rounded pixel rectangles. */
public final class GuiAssets {
    public static void mapPreview(GuiGraphics g,de.joris.aplejuice.BorderMap map,int x,int y,int w,int h) {
        float scale=Math.min(w/(float)map.imageWidth(),h/(float)map.imageHeight());
        int dw=Math.round(map.imageWidth()*scale),dh=Math.round(map.imageHeight()*scale);
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath("aplejuice","textures/maps/"+map.image()),
                x+(w-dw)/2,y+(h-dh)/2,0,0,dw,dh,map.imageWidth(),map.imageHeight(),map.imageWidth(),map.imageHeight());
    }
    public static void logo(GuiGraphics g, int x, int y, int size) {
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("aplejuice", "icon.png"),
                x, y, 0, 0, size, size, 64, 64, 64, 64);
    }
    public static void draw(GuiGraphics g, String name, int x, int y, int w, int h) {
        if (name.startsWith("eye_")) name = "eye";
        boolean flag = name.startsWith("flag_");
        int tw = flag ? 32 : 16, th = flag ? 24 : 16;
        float size = Math.min(w / (float) tw, h / (float) th);
        if (!flag && size >= 1) size = (float) Math.floor(size);
        int dw = Math.max(1, Math.round(tw * size)), dh = Math.max(1, Math.round(th * size));
        int left = x + (w - dw) / 2, top = y + (h - dh) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("aplejuice", "textures/gui/" + name + ".png"),
                left, top, 0, 0, dw, dh, tw, th, tw, th);
    }
    public static void rounded(GuiGraphics g, int x, int y, int w, int h, int r, int edge, int fill) {
        shape(g, x, y, w, h, r, edge); shape(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }
    private static void shape(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        int start = 0, previousInset = -1;
        for (int row = 0; row < h; row++) {
            int dy = Math.max(r - row - 1, row - (h - r));
            int inset = dy < 0 ? 0 : Math.max(0, r - (int) Math.sqrt(r * r - dy * dy));
            if (inset != previousInset) {
                if (row > start) g.fill(x + previousInset, y + start, x + w - previousInset, y + row, color);
                start = row; previousInset = inset;
            }
        }
        g.fill(x + previousInset, y + start, x + w - previousInset, y + h, color);
    }
}
