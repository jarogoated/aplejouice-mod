package de.joris.aplejuice.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Locale;

/** A persistent first-run greeting. No timeout: only actually opening the mod menu dismisses it. */
public final class WelcomeOverlay {
    private static final Identifier SKIN=Identifier.fromNamespaceAndPath("aplejuice","textures/gui/welcome_skin.png");
    private static final int W=310,H=104;
    private static long firstFrame;

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("aplejuice","welcome"),(g,ticks) -> {
            var client=Minecraft.getInstance();
            if (client.screen==null && client.level!=null && !client.options.hideGui) render(g);
        });
    }
    public static boolean visible() { return AplejuiceClient.config!=null && !AplejuiceClient.config.welcomeSeen; }
    public static boolean showAgain() {
        var config=AplejuiceClient.config;
        boolean previous=config.welcomeSeen;
        config.welcomeSeen=false;
        if (!config.save()) { config.welcomeSeen=previous; return false; }
        firstFrame=0;
        return true;
    }
    public static void menuOpened() {
        if (!visible()) return;
        AplejuiceClient.config.welcomeSeen=true;
        AplejuiceClient.config.save();
    }
    private static void render(GuiGraphics g) {
        if (!visible() || AplejuiceClient.menuKey==null) return;
        var client=Minecraft.getInstance();
        long now=System.nanoTime()/1_000_000;
        if (firstFrame==0) firstFrame=now;
        float enter=Math.min(1,(now-firstFrame)/450F);
        float slide=(1-enter)*(1-enter)*(W+16);
        // The vanilla boss bar spans 182 GUI pixels around the screen centre.
        float spaceBesideBoss=g.guiWidth()/2F-91-16;
        boolean narrow=spaceBesideBoss<140;
        float scale=Math.max(.1F,Math.min(.55F,Math.min(
                (narrow ? g.guiWidth()-16F : spaceBesideBoss)/W,(g.guiHeight()-16F)/H)));
        float top=narrow ? g.guiHeight()/3F+10 : 8;
        g.pose().pushMatrix();
        g.pose().translate(g.guiWidth()-W*scale-8+slide*scale,Math.min(top,g.guiHeight()-H*scale-8));
        g.pose().scale(scale,scale);
        GuiAssets.rounded(g,2,3,W,H,6,0x99000000,0x99000000);
        GuiAssets.rounded(g,0,0,W,H,6,0xFFB97839,0xFF251D19);
        g.fillGradient(5,5,W-5,46,0xFF513022,0x00251D19);
        g.fill(5,15,7,H-15,0xFFE78738);
        g.fillGradient(67,13,68,H-13,0xFFB97839,0xFF4B3225);
        renderSkin(g,now);
        g.drawString(client.font,Component.literal("Aplejouice Mod").withStyle(ChatFormatting.BOLD),78,15,0xFFFFB657,false);
        g.drawString(client.font,text("welcomeTitle"),78,32,0xFFFFEDDA,false);
        String key=AplejuiceClient.menuKey.getTranslatedKeyMessage().getString();
        String prompt=text(AplejuiceClient.menuKey.isUnbound() ? "welcomeUnbound" : "welcomePrompt",key);
        int y=51;
        for(var line:client.font.split(Component.literal(prompt),W-92)) {
            if (y>73) break;
            g.drawString(client.font,line,78,y,0xFFE5C7AA,false); y+=11;
        }
        g.drawString(client.font,text("welcomeTagline"),78,86,0xFFC68B56,false);
        GuiAssets.draw(g,"eye",W-26,12,14,14);
        g.pose().popMatrix();
    }
    private static String text(String key,Object... args) {
        // Only these four short strings are needed in the HUD; no language/resource reload on every frame.
        boolean english="en_us".equals(AplejuiceClient.config.language);
        String value=switch(key) {
            case "welcomeTitle" -> english ? "Your game. Your mod." : "Dein Spiel. Deine Mod.";
            case "welcomePrompt" -> english ? "Press %s to open your Aplejouice menu." : "Drücke %s und öffne dein Aplejouice-Menü.";
            case "welcomeUnbound" -> english ? "Set your menu key in Minecraft Controls." : "Lege deine Menütaste unter Steuerung fest.";
            default -> english ? "Made for your next round." : "Für deine nächste Runde.";
        };
        return String.format(Locale.ROOT,value,args);
    }
    /** Render the supplied skin directly as a pixel avatar; the right arm waves around its shoulder. */
    private static void renderSkin(GuiGraphics g,long now) {
        int x=30,y=25;
        part(g,x,y+40,4,20,4,12); part(g,x,y+40,4,36,4,12);
        part(g,x+8,y+40,20,52,4,12); part(g,x+8,y+40,4,52,4,12);
        part(g,x,y+16,20,20,8,12); part(g,x,y+16,20,36,8,12);
        part(g,x+16,y+16,36,52,4,12); part(g,x+16,y+16,52,52,4,12);
        g.pose().pushMatrix();
        g.pose().translate(x-2,y+18);
        g.pose().rotate(2.55F+.22F*(float)Math.sin(now/160.0));
        part(g,-4,0,44,20,4,12); part(g,-4,0,44,36,4,12);
        g.pose().popMatrix();
        part(g,x,y,8,8,8,8); part(g,x,y,40,8,8,8);
    }
    private static void part(GuiGraphics g,int x,int y,int u,int v,int w,int h) {
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,SKIN,x,y,u,v,w*2,h*2,w,h,64,64);
    }
    private WelcomeOverlay() {}
}
