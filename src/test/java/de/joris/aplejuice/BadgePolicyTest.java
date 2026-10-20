package de.joris.aplejuice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BadgePolicyTest {
    @Test void activeWatchedPlayerIsVisibleWithoutDependingOnVanillaNameTags() {
        assertTrue(BadgePolicy.visible(true, true, false, false, false, 4096, 64));
        assertFalse(BadgePolicy.visible(true, false, false, false, false, 16, 64));
        assertFalse(BadgePolicy.visible(false, true, false, false, false, 16, 64));
        assertFalse(BadgePolicy.visible(true, true, false, false, false, 4097, 64));
    }
    @Test void zeroVisibilityIsIndependentOfModeChangesAndCountsAppearImmediately() {
        assertTrue(BadgePolicy.showCount(true, 0));
        assertFalse(BadgePolicy.showCount(false, 0));
        assertTrue(BadgePolicy.showCount(false, 1));
        assertTrue(BadgePolicy.showCount(true, 700));
    }
    @Test void invisiblePlayersSelfAndHiddenHudRemainExcluded() {
        assertFalse(BadgePolicy.visible(true, true, true, false, false, 1, 64));
        assertFalse(BadgePolicy.visible(true, true, false, true, false, 1, 64));
        assertFalse(BadgePolicy.visible(true, true, false, false, true, 1, 64));
    }
}
