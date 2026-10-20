package de.joris.aplejuice;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NearestSelectionTest {
    private record Candidate(int id, double distance, boolean active) {}

    @Test void crowdedLobbyMatchesNearestEligiblePlayersWithoutRepeatedDistanceLookups() {
        var random = new Random(731);
        var lobby = new ArrayList<Candidate>();
        for (int i = 0; i < 10_000; i++) lobby.add(new Candidate(i, random.nextInt(5000), i % 7 != 0));
        var calls = new AtomicInteger();
        var actual = NearestSelection.within(lobby, 8, 4096, Candidate::active, candidate -> {
            calls.incrementAndGet(); return candidate.distance();
        });
        var expected = lobby.stream().filter(Candidate::active).filter(p -> p.distance() <= 4096)
                .sorted(Comparator.comparingDouble(Candidate::distance)).limit(8).toList();
        assertEquals(expected, actual);
        assertEquals(lobby.stream().filter(Candidate::active).count(), calls.get());
    }

    @Test void keepsStableTiesAndHandlesRangeBoundaryAndInvalidDistances() {
        var first = new Candidate(1, 10, true);
        var second = new Candidate(2, 10, true);
        var third = new Candidate(3, 10, true);
        var candidates = List.of(new Candidate(4, Double.NaN, true), first, second, third,
                new Candidate(5, 11, true), new Candidate(6, 1, false));
        assertEquals(List.of(first, second), NearestSelection.within(candidates, 2, 10, Candidate::active, Candidate::distance));
        assertTrue(NearestSelection.within(candidates, 8, 0, Candidate::active, Candidate::distance).isEmpty());
    }

    @Test void disabledSelectionDoesNotScanThePopulation() {
        Iterable<Integer> candidates = () -> { throw new AssertionError("Unexpected player scan"); };
        assertTrue(NearestSelection.within(candidates, 0, 64, p -> true, p -> p).isEmpty());
    }
}
