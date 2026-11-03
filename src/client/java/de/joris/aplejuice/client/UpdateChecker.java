package de.joris.aplejuice.client;

import de.joris.aplejuice.UpdateRelease;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.network.chat.Component;
import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** One bounded HTTPS check per process, never on Minecraft's main thread. No downloads or installation. */
public final class UpdateChecker {
    private static UpdateRelease available;
    private static boolean notified;
    public static UpdateRelease available() { return available; }
    public static void register() {
        var loader=FabricLoader.getInstance();
        String installed=loader.getModContainer("aplejuice").orElseThrow().getMetadata().getVersion().getFriendlyString();
        String minecraft=loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
        Thread.ofVirtual().name("aplejouice-update-check").start(() -> {
            HttpsURLConnection connection=null;
            try {
                connection=(HttpsURLConnection)URI.create("https://aplejouice.com/api/mod/latest?minecraft="+minecraft).toURL().openConnection();
                connection.setConnectTimeout(3000); connection.setReadTimeout(4000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept","application/json");
                connection.setRequestProperty("User-Agent","Aplejouice-Mod/"+installed);
                if(connection.getResponseCode()!=200 || connection.getContentType()==null
                        || !connection.getContentType().startsWith("application/json")) return;
                try(var stream=connection.getInputStream()) {
                    byte[] bytes=stream.readNBytes(16385);
                    if(bytes.length>16384) return;
                    var release=UpdateRelease.parse(new String(bytes,StandardCharsets.UTF_8),minecraft,installed);
                    if(release!=null) Minecraft.getInstance().execute(() -> available=release);
                }
            } catch(Exception ignored) { /* Offline or unavailable: never interrupt startup. */ }
            finally { if(connection!=null) connection.disconnect(); }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(available==null || notified) return;
            if(client.screen instanceof TitleScreen title) {
                notified=true; client.setScreen(new UpdatePromptScreen(title,available));
            } else if(client.screen==null && client.player!=null) {
                notified=true;
                client.gui.getChat().addMessage(Component.literal(
                        english() ? "Aplejouice Mod: update "+available.version()+" available. Download: "+UpdateRelease.DOWNLOAD_URL
                                : "Aplejouice Mod: Version "+available.version()+" verfügbar. Download: "+UpdateRelease.DOWNLOAD_URL));
            }
        });
    }
    public static boolean english() { return "en_us".equals(AplejuiceClient.config.language); }
    public static void openDownloads(Screen parent) { ConfirmLinkScreen.confirmLinkNow(parent,UpdateRelease.DOWNLOAD_URL); }
    private UpdateChecker() {}
}
