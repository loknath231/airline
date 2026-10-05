package com.airline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for airport details
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Entity
@Table(name = "airport")
@Getter
@Setter
@NoArgsConstructor
public class Airport {
    @Id
    @Column(length = 3)
    private String code;

    private String name;
    private String city;
    private String country;
}
