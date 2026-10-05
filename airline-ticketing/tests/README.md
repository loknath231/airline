# Tests
Tests follow the Maven convention and live in `src/test/java` (this folder documents them).

| Test | Type | What it proves |
|------|------|----------------|
| `FlightInstanceGeneratorTest` | unit | weekday matching, inclusive 365-day window; nightly `extendWindow()` carries on with the next schedule when one fails concurrently |
| `AircraftSeatMapTest` | unit | seat label generation, seat validation |
| `BookingWindowTest` | unit | booking window is `[today, today+365]` with inclusive bounds; dates outside are rejected |
| `GlobalExceptionHandlerTest` | unit | constraint violation -> 400, data integrity / optimistic lock / lock timeout -> 409, Spring `ErrorResponse` keeps its status and headers (405 + `Allow`), unexpected error -> 500 with generic message; timestamps come from the injected `Clock` |
| `DemoDataLoaderTest` | unit (Mockito) | demo seeding creates the 32 schedules only when the database has none |
| `SchemaFilesTest` | unit | `db/schema.sql` stays identical to the Flyway migration `db/migrations/V1__init_schema.sql` |
| `BookingConcurrencyIT` | integration | 20 threads fight for seat 1A -> exactly 1 wins; different seats booked concurrently all succeed; cancel releases seats and they can be re-booked |
| `BookingRulesIT` | integration | seat normalisation, seat not on aircraft, duplicate seat in request, cancelled flight, departed flight (409 on booking and cancel, hidden from search), unknown PNR, availability in search, idempotent nightly generation |
| `ApiFlowIT` | integration (MockMvc) | schedule -> search -> book -> double-book 409 -> seat map -> cancel -> idempotency 409 |
| `ApiErrorHandlingIT` | integration (MockMvc) | every HTTP error response: schedule validation/duplicates/unknown airport or aircraft, search parameter errors, unknown flight/PNR/schedule, malformed JSON, type mismatch, unknown URL 404, wrong method 405, unsupported media type 415; schedule `Location` header and `GET` by id; airport and schedule listing |

Seat counts are read from the seat map (`totalSeats`) instead of hard-coded, so tests keep passing when the aircraft
seed data changes. `com.airline.TestFlights` provides unique flight numbers and booking helpers. The integration tests
run against the in-memory H2 database migrated by Flyway, with demo seeding switched off (`app.seed-demo-data=false`,
set in the Surefire configuration).

Run: `mvnw.cmd test` / `./mvnw test` (all 55 tests), `mvnw.cmd verify` (tests + 90% coverage check, report in
`target/site/jacoco/index.html`).
