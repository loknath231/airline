package com.airline.service;

import static org.junit.jupiter.api.Assertions.*;

import com.airline.domain.Aircraft;
import org.junit.jupiter.api.Test;

class AircraftSeatMapTest {
    private Aircraft a320() {
        Aircraft a = new Aircraft();
        a.setAircraftType("A320");
        a.setTotalRows(30);
        a.setSeatLetters("ABCDEF");
        return a;
    }

    @Test
    void labelsFollowConfiguration() {
        var labels = a320().seatLabels();
        assertEquals(180, labels.size());
        assertEquals("1A", labels.get(0));
        assertEquals("1B", labels.get(1));
        assertEquals("30F", labels.get(179));
    }

    @Test
    void seatValidation() {
        Aircraft a = a320();
        assertTrue(a.hasSeat("1A"));
        assertTrue(a.hasSeat("30F"));
        assertFalse(a.hasSeat("31A"));
        assertFalse(a.hasSeat("0A"));
        assertFalse(a.hasSeat("1G"));
        assertFalse(a.hasSeat("A1"));
        assertFalse(a.hasSeat(""));
    }
}
