package de.joris.aplejuice;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TargetSelectionTest {
    @Test void opponentModeWaitsForAnExplicitAttackOrSelectionAndDoesNotActivateOnSpawn() {
        var selection = new TargetSelection(); var bot = UUID.randomUUID(); var opponent = UUID.randomUUID();
        selection.configure(true, true);
        assertFalse(selection.accepts(bot, true, true));
        selection.select(opponent, true);
        assertTrue(selection.accepts(opponent, true, true));
        assertFalse(selection.accepts(bot, true, true));
        selection.configure(true, true);
        assertEquals(opponent, selection.selected(), "Saving unrelated preferences retains the selection");
        selection.clear();
        assertFalse(selection.accepts(opponent, true, true), "New server/world has no previous opponent");
    }
    @Test void nearbyModeShowsOthersAndEnablingOpponentModeRequiresAFreshSelection() {
        var selection = new TargetSelection(); var first = UUID.randomUUID(); var other = UUID.randomUUID();
        selection.configure(true, false); selection.select(first, true);
        assertTrue(selection.accepts(other, true, false));
        selection.configure(true, true);
        assertNull(selection.selected()); assertFalse(selection.accepts(first, true, true));
        selection.select(other, true); assertTrue(selection.accepts(other, true, true));
        selection.configure(true, false); assertTrue(selection.accepts(first, true, false));
    }
    @Test void disablingTrackingClearsTheTargetAndAttacksWhileDisabledCannotPreselectIt() {
        var selection = new TargetSelection(); var opponent = UUID.randomUUID();
        selection.configure(true, true); selection.select(opponent, true);
        selection.configure(false, true); selection.select(opponent, false);
        assertNull(selection.selected()); assertFalse(selection.accepts(opponent, false, false));
        selection.configure(true, true); assertFalse(selection.accepts(opponent, true, true));
    }
}
