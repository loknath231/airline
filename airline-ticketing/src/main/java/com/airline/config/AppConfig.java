package com.airline.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AppConfig
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-10-04
 */
@Configuration
public class AppConfig {
    /** Injectable UTC clock so time-dependent logic is testable. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Airline Ticketing System API")
                                .version("1.0.0")
                                .description(
                                        "Single-airline reservation backend: schedules, search, seat maps, booking, cancellation."));
    }
}
