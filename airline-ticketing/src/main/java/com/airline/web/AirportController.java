package com.airline.web;

import com.airline.repository.AirportRepository;
import com.airline.web.dto.AirportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only reference data: airports
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-03
 */
@RestController
@RequestMapping("/api/v1/airports")
@RequiredArgsConstructor
@Tag(name = "Airports Reference data")
public class AirportController {
    private final AirportRepository airportRepository;

    /**
     * Lists the seeded airports. Airport CRUD is out of scope: new airports are added with a new
     * migration in {@code db/migrations}.
     *
     * @return all airports ordered by IATA code
     */
    @GetMapping
    @Operation(summary = "Airports List (read-only)")
    public List<AirportResponse> list() {
        return airportRepository.findAll(Sort.by("code")).stream()
                .map(AirportResponse::from)
                .toList();
    }
}
