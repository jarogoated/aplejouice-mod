package de.joris.aplejuice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UpdateReleaseTest {
    private String metadata(String version) {
        return "{\"schemaVersion\":1,\"modId\":\"aplejuice\",\"minecraft\":\"1.21.11\",\"version\":\""+version
                +"\",\"downloadUrl\":\"https://aplejouice.com/downloads\"}";
    }
    @Test void comparesVersionComponentsNumericallyAndOnlyOffersNewerBuilds() {
        assertTrue(UpdateRelease.newer("1.10.0","1.9.9"));
        assertTrue(UpdateRelease.newer("2.0.0","1.99.99"));
        assertFalse(UpdateRelease.newer("1.7.0","1.7.0"));
        assertFalse(UpdateRelease.newer("1.6.9","1.7.0"));
        assertNotNull(UpdateRelease.parse(metadata("1.7.0"),"1.21.11","1.6.0"));
        assertNull(UpdateRelease.parse(metadata("1.7.0"),"1.21.11","1.7.0"));
    }
    @Test void incompatibleMinecraftUntrustedLinksAndInvalidMetadataFailClosed() {
        String valid=metadata("1.7.0");
        assertNull(UpdateRelease.parse(valid,"1.22.0","1.6.0"));
        assertNull(UpdateRelease.parse(valid.replace("https://aplejouice.com/downloads","https://example.com/evil"),"1.21.11","1.6.0"));
        assertNull(UpdateRelease.parse(valid.replace("aplejuice","othermod"),"1.21.11","1.6.0"));
        for(String invalid:new String[]{"{}","{broken","[]","null","x".repeat(16385)})
            assertNull(UpdateRelease.parse(invalid,"1.21.11","1.6.0"));
    }
    @Test void devVersionsAndPrereleasesAreNotTreatedAsStableUpdates() {
        assertFalse(UpdateRelease.newer("1.8.0-beta.1","1.7.0"));
        assertFalse(UpdateRelease.newer("999999999999.0.0","1.7.0"));
        assertFalse(UpdateRelease.newer("1.8.0",null));
        assertFalse(UpdateRelease.newer("1.8.0","development"));
    }
}
