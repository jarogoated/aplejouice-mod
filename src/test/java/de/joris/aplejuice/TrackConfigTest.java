package de.joris.aplejuice;

import de.joris.aplejuice.client.TrackConfig;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TrackConfigTest {
    @TempDir Path directory;
    @Test void everyPreferenceSurvivesSavingAndReloadingAcrossSessions() {
        Path path = directory.resolve("nested/settings.json");
        var settings = new TrackConfig(); settings.enabled = true; settings.opponentOnly = true;
        settings.showZero = false; settings.range = 64; settings.language = "en_us";
        settings.items = new ArrayList<>(List.of("minecraft:totem_of_undying", "minecraft:end_crystal"));
        assertTrue(settings.save(path));
        for (int connection = 0; connection < 3; connection++) {
            var reloaded = TrackConfig.load(path);
            var session = new TargetSelection(); session.configure(reloaded.enabled, reloaded.opponentOnly);
            session.select(UUID.randomUUID(), reloaded.enabled); session.clear();
            assertNull(session.selected());
            assertTrue(reloaded.enabled); assertTrue(reloaded.opponentOnly); assertFalse(reloaded.showZero);
            assertEquals(64, reloaded.range); assertEquals("en_us", reloaded.language);
            assertEquals(settings.items, reloaded.items);
            assertTrue(reloaded.save(path));
        }
        var last = TrackConfig.load(path); last.enabled = false; last.opponentOnly = false; last.showZero = true;
        assertTrue(last.save(path));
        var reloaded = TrackConfig.load(path);
        assertFalse(reloaded.enabled); assertFalse(reloaded.opponentOnly); assertTrue(reloaded.showZero);
    }
    @Test void corruptFileRecoversPreviousValidSettingsAndNoTemporaryFilesRemain() throws Exception {
        Path path = directory.resolve("settings.json");
        var settings = new TrackConfig(); settings.opponentOnly = true; settings.range = 64;
        assertTrue(settings.save(path)); assertTrue(settings.save(path));
        Files.writeString(path, "{broken");
        var reloaded = TrackConfig.load(path);
        assertTrue(reloaded.opponentOnly); assertEquals(64, reloaded.range);
        assertTrue(reloaded.save(path));
        try (var files = Files.list(directory)) { assertTrue(files.noneMatch(file -> file.toString().endsWith(".tmp"))); }
    }
    @Test void olderConfigsKeepDefaultsAndUnwritableDestinationReportsFailure() throws Exception {
        Path path = directory.resolve("settings.json"); Files.writeString(path, "{}");
        var defaults = TrackConfig.load(path);
        assertTrue(defaults.enabled); assertTrue(defaults.showZero); assertFalse(defaults.opponentOnly);
        Path parentFile = directory.resolve("not-a-directory"); Files.writeString(parentFile, "unchanged");
        assertFalse(defaults.save(parentFile.resolve("settings.json")));
        assertEquals("unchanged", Files.readString(parentFile));
    }
}
