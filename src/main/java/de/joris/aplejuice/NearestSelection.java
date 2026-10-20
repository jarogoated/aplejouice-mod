package de.joris.aplejuice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Keep only the small nearest set instead of allocating and sorting the entire population. */
public final class NearestSelection {
    private NearestSelection() {}

    public static <T> List<T> within(Iterable<T> candidates, int limit, double maxDistance,
                                    Predicate<T> eligible, ToDoubleFunction<T> distance) {
        if (limit <= 0) return List.of();
        var nearest = new ArrayList<T>(limit);
        var distances = new double[limit];
        for (T candidate : candidates) {
            if (!eligible.test(candidate)) continue;
            double value = distance.applyAsDouble(candidate);
            if (!Double.isFinite(value) || value > maxDistance) continue;
            int index = nearest.size();
            while (index > 0 && value < distances[index - 1]) index--;
            if (index >= limit) continue;
            if (nearest.size() == limit) nearest.remove(limit - 1);
            nearest.add(index, candidate);
            System.arraycopy(distances, index, distances, index + 1, nearest.size() - index - 1);
            distances[index] = value;
        }
        return List.copyOf(nearest);
    }
}
