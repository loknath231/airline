package com.airline.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for aircraft details
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "aircraft")
@Getter
@Setter
@NoArgsConstructor
public class Aircraft {
    private static final Pattern SEAT = Pattern.compile("^(\\d{1,2})([A-Z])$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aircraftType;
    private int totalRows;
    /** Seat letters per row, e.g. "ABCDEF". */
    private String seatLetters;

    public int capacity() {
        return totalRows * seatLetters.length();
    }

    /** All seat labels, row-major: 1A,1B,...,30F. */
    public List<String> seatLabels() {
        List<String> labels = new ArrayList<>(capacity());
        for (int r = 1; r <= totalRows; r++) {
            for (char c : seatLetters.toCharArray()) labels.add(r + String.valueOf(c));
        }
        return labels;
    }

    public boolean hasSeat(String label) {
        Matcher m = SEAT.matcher(label);
        if (!m.matches()) return false;
        int row = Integer.parseInt(m.group(1));
        return row >= 1 && row <= totalRows && seatLetters.indexOf(m.group(2).charAt(0)) >= 0;
    }
}
