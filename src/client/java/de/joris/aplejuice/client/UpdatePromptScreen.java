package de.joris.aplejuice.client;

import de.joris.aplejuice.UpdateRelease;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A one-time startup notice with an explicit browser link and a normal dismiss action. */
public final class UpdatePromptScreen extends Screen {
    private final Screen parent;
    private final UpdateRelease release;
    public UpdatePromptScreen(Screen parent,UpdateRelease release) {
        super(Component.literal("Aplejouice Mod Update")); this.parent=parent; this.release=release;
    }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal(UpdateChecker.english() ? "Download update" : "Update herunterladen"),
                button -> UpdateChecker.openDownloads(this)).bounds(width/2-144,height/2+38,140,20).build());
        addRenderableWidget(Button.builder(Component.literal(UpdateChecker.english() ? "Later" : "Später"),
                button -> onClose()).bounds(width/2+4,height/2+38,140,20).build());
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) {
        g.fillGradient(0,0,width,height,0xFF241C18,0xFF101114);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        int x=width/2,y=height/2;
        GuiAssets.rounded(g,x-154,y-88,308,158,6,0xFF9F653A,0xFF29211C);
        GuiAssets.logo(g,x-16,y-77,32);
        g.drawCenteredString(font,"Aplejouice Mod",x,y-36,0xFFFFBD69);
        g.drawCenteredString(font,UpdateChecker.english() ? "A new version is available" : "Eine neue Version ist verfügbar",x,y-17,0xFFFFEDDA);
        g.drawCenteredString(font,release.installed()+"  →  "+release.version(),x,y+1,0xFFD2B397);
        g.drawCenteredString(font,"aplejouice.com",x,y+20,0xFFFFBD69);
        super.render(g,mx,my,delta);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
