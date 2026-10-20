package de.joris.aplejuice;

/** Visibility does not depend on vanilla's name-tag text or on counters being nonzero. */
public final class BadgePolicy {
    private BadgePolicy() {}
    public static boolean visible(boolean enabled, boolean watched, boolean self, boolean invisible,
                                  boolean hideGui, double distanceSquared, int range) {
        return enabled && watched && !self && !invisible && !hideGui && distanceSquared <= (double) range * range;
    }
    public static boolean showCount(boolean showZero, long count) { return showZero || count > 0; }
}
