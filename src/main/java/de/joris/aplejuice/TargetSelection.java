package de.joris.aplejuice;

import java.util.UUID;

/** Session-only selection. Choosing a player never changes persistent preferences. */
public final class TargetSelection {
    private UUID selected;
    private boolean previousOpponentOnly;
    public UUID selected() { return selected; }
    public void clear() { selected = null; }
    public void configure(boolean enabled, boolean opponentOnly) {
        if (!enabled || opponentOnly && !previousOpponentOnly) clear();
        previousOpponentOnly = opponentOnly;
    }
    public void select(UUID id, boolean enabled) { if (enabled) selected = id; }
    public boolean accepts(UUID id, boolean enabled, boolean opponentOnly) {
        return enabled && (!opponentOnly || id.equals(selected));
    }
}
