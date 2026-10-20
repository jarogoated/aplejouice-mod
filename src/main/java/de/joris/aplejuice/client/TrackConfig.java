package de.joris.aplejuice.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Persistent preferences; server sessions and selected opponents are deliberately separate. */
public final class TrackConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Runnable onChanged = () -> {};
    public List<String> items = new ArrayList<>(List.of("minecraft:cobweb"));
    public boolean enabled = true;
    public boolean showZero = true;
    public boolean opponentOnly = false;
    public int range = 48;
    public String language = "de_de";
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("aplejuice-item-track.json"); }
    public static void onChanged(Runnable listener) { onChanged = Objects.requireNonNull(listener); }
    public static TrackConfig load() { return load(path()); }
    public static TrackConfig load(Path path) {
        for (Path candidate : List.of(path, backup(path))) {
            if (!Files.exists(candidate)) continue;
            try {
                TrackConfig config = GSON.fromJson(Files.readString(candidate), TrackConfig.class);
                if (config == null) throw new IOException("Empty settings document");
                config.normalize(); return config;
            } catch (Exception e) { LoggerFactory.getLogger("aplejuice").warn("Could not load settings from {}", candidate, e); }
        }
        return new TrackConfig();
    }
    private void normalize() {
        if (items == null) items = new ArrayList<>();
        items = new ArrayList<>(items.stream().filter(Objects::nonNull).distinct().limit(8).toList());
        range = Math.clamp(range, 8, 64);
        if (!"en_us".equals(language)) language = "de_de";
    }
    private static Path backup(Path path) { return path.resolveSibling(path.getFileName() + ".bak"); }
    public boolean save() { return save(path()); }
    public boolean save(Path path) {
        onChanged.run();
        Path temp = null;
        try {
            path = path.toAbsolutePath();
            Files.createDirectories(path.getParent());
            temp = Files.createTempFile(path.getParent(), "aplejouice-settings-", ".tmp");
            Files.writeString(temp, GSON.toJson(this));
            if (Files.exists(path)) {
                // Keep only a parseable previous file as the recovery copy.
                try {
                    if (GSON.fromJson(Files.readString(path), TrackConfig.class) != null)
                        Files.copy(path, backup(path), StandardCopyOption.REPLACE_EXISTING);
                } catch (com.google.gson.JsonParseException ignored) { }
            }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
            return true;
        } catch (Exception e) {
            LoggerFactory.getLogger("aplejuice").error("Could not save settings", e); return false;
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
        }
    }
}
