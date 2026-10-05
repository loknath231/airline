# Airline Ticketing System

Backend for a single-airline reservation system: schedule management, flight search, live seat maps,
booking and cancellation. Built with **Java 21, Spring Boot 4, Maven, Spring Data JPA (Hibernate 7), H2, springdoc Swagger UI**.

| URL | Purpose |
|-----|---------|
| http://localhost:8080/swagger-ui.html | Swagger UI (try every API) |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/h2-console | H2 console - JDBC URL `jdbc:h2:mem:airline`, user `sa`, empty password |

## Setup

**Dependencies:** JDK 21 or newer (tested on 21 and 26) and internet access to Maven Central. Maven itself is optional:
the included Maven Wrapper (`mvnw` / `mvnw.cmd`) downloads Maven 3.9.16 on first use. Docker is optional. The build
checks the JDK and Maven versions (`maven-enforcer-plugin`) and fails with a clear message if they are too old.

```bash
./mvnw clean package       # build + run tests            (Windows: mvnw.cmd clean package)
./mvnw clean verify        # build + tests + enforce >= 90% code coverage
./mvnw spring-boot:run     # run on :8080
# or
java -jar target/airline-ticketing-1.0.0.jar
# or
docker compose up --build
```

**Database initialisation:** automatic on start-up with **Flyway**, from the single source of truth in
[`db/migrations/`](db/migrations): `V1__init_schema.sql` creates tables, constraints and indexes, and
`V2__seed_airports_aircraft.sql` seeds **55 airports** and **100 aircraft** (from the A318 to the A380, with their seat
layouts). With `app.seed-demo-data=true` (default) **32 demo schedules** (XY101–XY506, e.g. DXB→LHR, DXB→CDG, LHR→JFK)
are created a few seconds after start-up so search works immediately. `db/schema.sql` is a readable standalone copy of
the schema; `SchemaFilesTest` fails the build if it drifts from `V1`. For a persistent DB change the URL in
`application.yml` to `jdbc:h2:file:./data/airline`.

**Profiles:** the default profile is tuned for evaluation (H2 console, Swagger UI, demo schedules). Run with
`--spring.profiles.active=prod` (or `SPRING_PROFILES_ACTIVE=prod`) to switch all three off.

## Quick walk-through (curl)

```bash
# 1. create a schedule (back office) - generates 365 days of instances; Location header points to the new schedule
#    (aircraftId 3 = A320, 30 rows x ABCDEF; XY1xx-XY5xx are taken by the demo schedules)
curl -i -X POST localhost:8080/api/v1/admin/schedules -H 'Content-Type: application/json' -d '{
 "flightNumber":"XY901","sourceAirport":"DXB","destinationAirport":"BOM","departureTime":"09:30",
 "arrivalTime":"14:15","aircraftId":3,"daysOfOperation":["MONDAY","WEDNESDAY","FRIDAY"]}'
# 2. search (UTC date within the next 365 days; XY103 DXB->CDG flies daily)
curl 'localhost:8080/api/v1/flights/search?source=DXB&destination=CDG&date=<YYYY-MM-DD>'
# 3. seat map of a flightInstanceId returned by search
curl localhost:8080/api/v1/flights/<flightInstanceId>/seats
# 4. book
curl -X POST localhost:8080/api/v1/bookings -H 'Content-Type: application/json' -d '{
 "flightInstanceId":<flightInstanceId>,"passengers":[{"name":"Ann","seat":"1A"},{"name":"Ben","seat":"1B"}]}'
# 5. cancel
curl -X POST localhost:8080/api/v1/bookings/<PNR>/cancel
```

## API summary

| Method & path | Description | Errors |
|---|---|---|
| `POST /api/v1/admin/schedules` | create schedule + generate instances (201 + `Location`) | 400, 404 (airport/aircraft), 409 (dup flight no.) |
| `GET /api/v1/admin/schedules/{id}` | one schedule | 404 |
| `GET /api/v1/admin/schedules` | list schedules | |
| `GET /api/v1/flights/search?source&destination&date` | flights on that date with availability | 400, 404 |
| `GET /api/v1/flights/{id}/seats` | seat map (AVAILABLE/BOOKED) | 404 |
| `POST /api/v1/bookings` | book 1-9 seats (201 + `Location`) | 400, 404, 409 (seat taken, flight closed or departed, flight busy) |
| `GET /api/v1/bookings/{pnr}` | booking details | 404 |
| `POST /api/v1/bookings/{pnr}/cancel` | cancel + release seats | 404, 409 |
| `GET /api/v1/airports` | seeded airports (helper) | |

Errors share one JSON shape: `{timestamp,status,error,message,path,details[]}`. Framework errors keep their real status:
unknown URL **404**, wrong method **405** (with `Allow` header), unsupported `Content-Type` **415**. Only genuinely
unexpected failures return **500**, with a generic message (details are logged, not exposed).

## Design decisions

* **Layered architecture** (web -> service -> repository -> domain). Controllers only translate HTTP; business rules and
  transaction boundaries live in services; entities hold small invariants (`Aircraft.hasSeat`, `Booking.cancel`).
* **Flight instances: hybrid, materialised.** Instances are rows generated for `[today, today+365]` when a schedule is
  created and extended by a nightly job (`FlightInstanceGenerator.extendWindow`). Materialising gives each flight a stable
  id to hang bookings/locks on and makes search a simple indexed query. Generation is idempotent.
* **Seat inventory is derived, not stored per seat.** The seat map is the fixed aircraft layout overlaid with
  `booking_seat` rows - no 180 x 365 pre-created seat rows per schedule.
* **UTC everywhere**; `arrival_day_offset` models overnight flights; time-zone conversion is out of scope.
* **Seat release without losing history:** `booking_seat.active_seat_label` is NULL once released, and
  `UNIQUE(flight_instance_id, active_seat_label)` only constrains held seats.

## Search algorithm

1. Validate source/destination (exist, differ) and that the date is within `[today, today+365]` (UTC).
2. *Flights identified / schedules matched:* query `flight_instance` joined to `flight_schedule` on
   `source, destination, flight_date, status=SCHEDULED` (operating-day matching already happened at generation time).
3. *Instance generation:* for every date in the window whose `DayOfWeek` is in the schedule's days, insert
   `(schedule, date, departure_utc, arrival_utc)` unless it exists (`UNIQUE(schedule_id, flight_date)`).
4. *Availability:* `capacity (rows x letters) - COUNT(booking_seat WHERE active_seat_label IS NOT NULL)`, grouped for all
   results in one query (no N+1). Flights that have already departed are hidden.

## Booking algorithm

1. Open a transaction and `SELECT ... FOR UPDATE` the `flight_instance` row (pessimistic lock).
2. Validate: flight open, in booking window, not departed; each seat exists on the aircraft (regex + rows/letters); no
   seat repeated in the request.
3. Read active seats of the flight; if any requested seat is taken -> `409` listing them, nothing is written.
4. Insert `booking` (UUID PNR, `CONFIRMED`) and one `booking_seat` per passenger; flush; commit (lock released).
5. **Concurrency (defence in depth):** (a) row lock serialises bookers of the *same* flight while different flights run in
   parallel; (b) DB unique constraint on `(flight_instance_id, active_seat_label)` guarantees no double booking even if
   application logic were bypassed (mapped to 409); (c) `@Version` on `booking` guards concurrent modification.
   Proven by `BookingConcurrencyIT` (20 threads, 1 winner). A request that waits longer than `LOCK_TIMEOUT` (10 s)
   for the flight lock gets **409** "Flight is busy with other bookings, please retry".

## Cancellation algorithm

1. Find the flight id for the PNR (404 if unknown), lock that flight row, then load the booking (so we see the latest state).
2. Reject if already `CANCELLED` or the flight has departed (409).
3. Set `status=CANCELLED`, `cancelled_at`, and `active_seat_label=NULL` on every seat; commit. Seats instantly show
   as AVAILABLE and can be re-booked. Refunds are out of scope.

## Assumptions

* Single airline; airports and aircraft are pre-loaded; schedules are immutable after creation.
* One booking = one flight instance; one seat belongs to at most one active booking; 1-9 passengers per booking.
* All times UTC; arrival is after departure (use `arrivalDayOffset` for next-day arrival, max 3).
* No authentication/authorisation (back-office endpoints are open); no payments, no passenger validation beyond a name.
* Booking is allowed until departure time; bookings/cancellations after departure are rejected.
* The H2 console and in-memory DB are for evaluation convenience only.

## Repository layout

```
README.md  pom.xml  mvnw  mvnw.cmd  .mvn/  lombok.config  Dockerfile  docker-compose.yml
hld/architecture.pdf  hld/architecture.md  hld/diagrams/*.{mmd,svg,png}
lld/design.md
db/schema.sql  db/migrations/V1__init_schema.sql  db/migrations/V2__seed_airports_aircraft.sql   (Flyway)
src/main/java/com/airline/{config,domain,repository,service,web,exception}
src/main/resources/application.yml  application-prod.yml
src/test/java/...   tests/README.md
```

## Tests and code coverage

`mvn test` runs **55 tests**: unit tests (`*Test`) and Spring Boot integration tests (`*IT`, against the in-memory H2
database migrated by Flyway). They cover instance generation, seat layout, the booking window, every API error response
(400/404/405/409/415/500), booking rules (unknown/duplicate seats, cancelled or departed flights), concurrency, the
nightly job, demo seeding, schema-file consistency and the full API flow. Demo seeding is switched off for the test run
(`app.seed-demo-data=false` in the Surefire configuration) to keep tests fast. See [tests/README.md](tests/README.md)
for the list.

**Code coverage target: 90%.** [JaCoCo](https://www.jacoco.org/jacoco/) measures coverage on every test run, and
`mvn verify` fails the build if line or instruction coverage drops below **90%**. The threshold is the
`jacoco.minimum.coverage` property in `pom.xml`.

| Metric | Coverage |
|---|---|
| Lines | 99.5% (401/403) |
| Instructions | 99.2% |
| Branches | 93.1% |

### Run all tests with coverage in one go

1. Open a terminal in the project folder:
   ```bash
   cd D:\vision\airline-ticketing
   ```
2. Run every test (unit + integration), measure coverage and enforce the 90% minimum. This works on any JDK from 21
   up (Lombok is declared as an annotation processor, so no extra flags are needed):
   ```bash
   mvnw.cmd clean verify
   ```
   (`./mvnw clean verify` on macOS/Linux, or `mvn clean verify` with a local Maven 3.9+.)
   A successful run ends with:
   ```
   [INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
   [INFO] All coverage checks have been met.
   [INFO] BUILD SUCCESS
   ```
3. Open the HTML coverage report (per package/class, lines highlighted green = tested, red = not tested):
   ```bash
   start target\site\jacoco\index.html
   ```

`mvn test` runs the same tests and writes the report without enforcing the minimum. The `*IT` tests start the
Spring Boot application themselves (`@SpringBootTest`), so the app does not need to be running.

**In IntelliJ:** enable *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation
processing*, then right-click `src/test/java` → *Run 'All Tests' with Coverage*.

Lombok-generated getters/setters are excluded (`lombok.config` marks them `@Generated`), as is the `main()` launcher
class. The two uncovered lines are the `DataIntegrityViolationException` fallback in `BookingService`, which the
per-flight row lock makes unreachable in practice.

## Code formatting

[Spotless](https://github.com/diffplug/spotless) keeps the code readable and consistently formatted, so reviews focus on
logic rather than whitespace. Java sources use **Palantir Java Format** (4-space indent), with unused imports removed,
trailing whitespace trimmed, a final newline and Unix (LF) line endings.

```bash
mvn spotless:apply         # format all sources in place
mvn spotless:check         # fail if any file is not formatted (use in CI / before committing)
```

Spotless is not bound to the build lifecycle, so `mvn package` does not fail on formatting; run `spotless:apply`
before committing.

## Code review by Lead

**Reviewer:** Java Microservices Lead. **Reviewed against:** *Backend Engineering Assignment – Airline Ticketing
System*. **First review:** 2026-10-04.

This section is a living record. New findings are appended to the tables below with the next free ID, and the
**Status** column is updated as items are fixed (`Open` → `Fixed`, with the date), never deleted.

### Verdict

**Approved** (updated 2026-10-04, after the fix round). All functional requirements are implemented and the core of the
system, booking under concurrency, is designed well: a per-flight `SELECT … FOR UPDATE` lock backed by a unique
constraint and proven by a 20-thread test. Every deliverable folder in the expected repository structure exists.

All **High** and **Medium** findings are fixed, plus five of the seven **Low** ones. Two Low items are partly fixed,
with the remaining work listed as future work: ShedLock (**L5**) and sequence-based IDs (**L4**). One step of **L6**
is left to the author: initialise git and commit.

*Initial verdict (first review): Approve with changes. Fix the High findings (wrong HTTP status codes, build not
compiling on a current JDK) and the stale documentation (M5) before submission.*

### Requirements compliance

| # | Assignment requirement | Status | Evidence |
|---|---|---|---|
| 1 | Airports seeded (IATA code, name, city, country); no CRUD | ✅ Met | `db/migrations/V2…` (55 airports), read-only `GET /api/v1/airports` |
| 2 | Aircraft pre-configured with fixed seat map; no CRUD | ✅ Met | `db/migrations/V2…` (100 aircraft), `Aircraft.seatLabels()/hasSeat()` |
| 3 | Back-office API to create schedules (flight no., route, times, aircraft, days) | ✅ Met | `POST /api/v1/admin/schedules`, `ScheduleService.create` |
| 4 | Bookable instances for 365 days; strategy documented | ✅ Met | Hybrid/materialised, `FlightInstanceGenerator` + nightly job; HLD §5, §7 |
| 5 | Search by source, destination, date returning flight no., times, available seats | ✅ Met | `FlightSearchService.search`, one grouped count query (no N+1) |
| 6 | Seat map with Available/Booked, labels per aircraft, real time | ✅ Met | `SeatMapService.get` reads live `booking_seat` rows |
| 7 | Booking returns PNR, flight no., date, passenger count, seats, status | ✅ Met | `BookingResponse` |
| 8 | No double booking; concurrent requests consistent; strategy documented | ✅ Met | Row lock + `UNIQUE(flight_instance_id, active_seat_label)` + `@Version`; `BookingConcurrencyIT` |
| 9 | Cancellation changes status and releases seats for rebooking | ✅ Met | `Booking.cancel` nulls `active_seat_label`; tested |
| NFR | Clean architecture, REST, transactions, validation, logging, testability | ✅ Met | Layering and transactions are good; error-handling gaps **H1** and **M1** fixed |
| D1 | HLD: architecture, components, flows, decisions, concurrency, generation | ✅ Met | `hld/architecture.md` + `.pdf`, diagrams |
| D2 | LLD: modules, classes, APIs, validation, errors, transactions | ✅ Met | `lld/design.md` |
| D3 | Executable implementation with clear setup | ✅ Met | Builds on JDK 21–26 with plain `mvnw clean verify` (**H2** fixed) |
| D4 | Schema: ER diagram, SQL, PK/FK/constraints/indexes | ✅ Met | `hld/diagrams/er-diagram.*`, `db/schema.sql`, `db/migrations/` (applied by Flyway) |
| D5 | README: setup, design, search/booking/cancel algorithms, assumptions | ✅ Met | All sections present; seed-data description corrected (**M5** fixed) |

### Findings

Severity: **High** = wrong behaviour or blocks evaluation; **Medium** = should fix before submission; **Low** =
quality improvement.

| ID | Severity | Area | Finding | Recommendation | Status |
|---|---|---|---|---|---|
| H1 | High | Error handling | The catch-all `@ExceptionHandler(Exception.class)` ([GlobalExceptionHandler.java:80](src/main/java/com/airline/exception/GlobalExceptionHandler.java:80)) also catches Spring MVC's own exceptions. **Verified on the running app:** an unknown URL returns **500** instead of 404, a wrong method (`DELETE /api/v1/airports`) **500** instead of 405, and `Content-Type: text/plain` on `POST /api/v1/bookings` **500** instead of 415. Each one is also logged at ERROR level with a stack trace. | Extend `ResponseEntityExceptionHandler` (or handle `ErrorResponse`/`ErrorResponseException` and keep its status code) so the catch-all only receives truly unexpected errors. Add `ApiErrorHandlingIT` cases for 404/405/415. | ✅ Fixed 2026-10-04: the catch-all now keeps the status of Spring `ErrorResponse` exceptions (and their headers, e.g. `Allow`); verified on the running jar: 404 / 405 / 415. New tests in `ApiErrorHandlingIT` and `GlobalExceptionHandlerTest`. |
| H2 | High | Build | `pom.xml` targets Java 21, but on JDK 23+ `javac` no longer runs annotation processors found on the classpath, so Lombok is skipped and the build fails with hundreds of "cannot find symbol" errors. The README currently works around this with `-Dmaven.compiler.proc=full`. An evaluator on a recent JDK cannot build the project as described. | Declare Lombok in `maven-compiler-plugin` `<annotationProcessorPaths>` (works on every JDK). Add the Maven Wrapper (`mvnw`) and a `maven-enforcer-plugin` `requireJavaVersion` rule so builds are reproducible. | ✅ Fixed 2026-10-04: Lombok declared in `annotationProcessorPaths`; Maven Wrapper (Maven 3.9.16) and `maven-enforcer-plugin` (JDK ≥ 21, Maven ≥ 3.9) added. Plain `mvn clean verify` passes on JDK 26. |
| M1 | Medium | Concurrency / errors | Lock waits are capped by `LOCK_TIMEOUT=10000` ([application.yml:6](src/main/resources/application.yml:6)). When a booking waits longer, Spring throws `PessimisticLockingFailureException`/`CannotAcquireLockException`, which no handler maps, so the client gets **500** (found by code reading). | Map lock-timeout exceptions to **409** (or **503** with `Retry-After`) and document that the client should retry. | ✅ Fixed 2026-10-04: `PessimisticLockingFailureException` mapped to **409** "Flight is busy with other bookings, please retry"; documented in the booking algorithm; unit-tested. |
| M2 | Medium | Security / config | The H2 web console ([application.yml:11](src/main/resources/application.yml:11)) and demo seeding (`app.seed-demo-data: true`) are enabled in the default profile, and the back-office endpoints are unauthenticated. Acceptable for the assignment, but a production deploy of this config exposes the database. | Move the H2 console and demo data to a `dev` profile and keep the default profile safe. Note in the HLD that `/api/v1/admin/**` would sit behind an admin role at the API gateway. | ✅ Fixed 2026-10-04: new `prod` profile (`application-prod.yml`) turns off the H2 console, Swagger UI and demo data; verified both profiles on the running jar. The default stays evaluator-friendly by choice. API-gateway auth remains a production recommendation. |
| M3 | Medium | Database | The schema exists three times (`src/main/resources/schema.sql`, `db/schema.sql`, `db/migrations/V1__init_schema.sql`) and the seed data twice (`data.sql`, `V2__seed_airports_aircraft.sql`). They match today, but nothing keeps them in sync. The `V1__`/`V2__` names suggest Flyway, but Flyway is not used. | Add Flyway, point it at `db/migrations`, and delete the `spring.sql.init` copies so there is one source of truth. | ✅ Fixed 2026-10-04: Flyway applies `db/migrations` (packaged as `classpath:db/migration`); `src/main/resources/schema.sql` and `data.sql` removed; `SchemaFilesTest` fails the build if `db/schema.sql` drifts from `V1`. |
| M4 | Medium | Code quality | Several Javadoc comments are copy-paste errors: `FlightController` search and seat-map methods say *"Processes a financial withdrawal from the account"* (lines 31, 48); `AdminScheduleController.createFlightSchedule` says *"List of Schedules of Flight"* (line 29); `BookingController.get` says *"Create a booking"* (line 43); `FlightInstanceGenerator.generate` says *"Pure date matching"* and uses the invalid tag `@Return` (lines 46, 58). An evaluator scoring readability will notice these. | Correct the comments, or delete them where the method name already says it. Remove the `// Fixed …` notes in `DemoDataLoader` (lines 67, 104, 115). | ✅ Fixed 2026-10-04: Javadoc rewritten in all controllers, `BookingService` and `FlightInstanceGenerator` (invalid `@Return` tags removed); `// Fixed …` notes removed from `DemoDataLoader`. |
| M5 | Medium | Documentation | README *Setup → Database initialisation* still says `data.sql` seeds *"8 airports and 3 aircraft (A320 30x…)"* and *"three demo schedules (XY101, XY102, XY201)"*. The real seed data has 55 airports, 100 aircraft and 32 demo schedules, and aircraft id 1 is now an A318. | Update the paragraph, and the curl walk-through if it relies on aircraft ids. | ✅ Fixed 2026-10-04: Setup section now describes Flyway, 55 airports, 100 aircraft, 32 demo schedules and profiles; the curl walk-through no longer reuses a demo flight number or fixed ids. |
| L1 | Low | API design | `GET /api/v1/airports` returns the JPA entity `Airport` directly ([AirportController.java:35](src/main/java/com/airline/web/AirportController.java:35)), coupling the API contract to the persistence model. Every other endpoint uses a DTO. | Return an `AirportResponse` record. | ✅ Fixed 2026-10-04: `AirportResponse` record, sorted by IATA code. |
| L2 | Low | API consistency | A booking on a departed flight returns **400** (`InvalidRequestException`), but cancelling a booking on a departed flight returns **409** (`ConflictException`). This is the same business rule with two status codes. | Use **409** for both, since it is a state conflict, not a malformed request. | ✅ Fixed 2026-10-04: booking a departed flight now returns **409**, consistent with cancel; `BookingRulesIT` updated. |
| L3 | Low | API design | `POST /api/v1/admin/schedules` returns 201 without a `Location` header, unlike bookings. `GET` list endpoints have no pagination. | Add `Location: /api/v1/admin/schedules/{id}` (with a `GET` by id). Paginate lists once data grows. | ✅ Fixed 2026-10-04 (Location + `GET /api/v1/admin/schedules/{id}`, tested). Pagination deferred until data volume needs it. |
| L4 | Low | Performance | Entities use `GenerationType.IDENTITY`, which stops Hibernate batching inserts, so each schedule creates up to 366 instance rows one statement at a time. Startup seeding of 32 schedules is about 9,000 single inserts, and this also slows every Spring Boot test context. | Use a `SEQUENCE` with an allocation size plus `hibernate.jdbc.batch_size`. Disable demo seeding in tests. | ◐ Partly fixed 2026-10-04: demo seeding disabled in tests (`DemoDataLoaderTest` covers the loader with mocks). `SEQUENCE` ids + JDBC batching left as future work: it changes the schema of every table. |
| L5 | Low | Scheduling | `extendWindow()` runs on every app instance and processes all schedules in one transaction. With more than one replica the nightly jobs race: the unique constraint keeps data correct, but one instance's whole transaction fails. | Use ShedLock (or a single scheduler instance) and commit per schedule. | ◐ Partly fixed 2026-10-04: `extendWindow()` no longer runs in one transaction; each schedule commits on its own and a concurrent-generation failure skips only that schedule (unit-tested). ShedLock left as future work for multi-replica deployments. |
| L6 | Low | Repo hygiene | The project is not a git repository. Stray H2 files `data.mv.db` / `data.trace.db` in the project root are not covered by `.gitignore`, which only ignores `data/`. | `git init`, add `*.mv.db` and `*.trace.db` to `.gitignore`, and commit the deliverables. | ◐ Partly fixed 2026-10-04: `.gitignore` now ignores `*.mv.db` / `*.trace.db`. Action for the author: `git init` and commit the deliverables. |
| L7 | Low | Testability | `GlobalExceptionHandler` stamps errors with `Instant.now()` instead of the injected `Clock` used everywhere else. | Inject `Clock` for consistency. | ✅ Fixed 2026-10-04: `GlobalExceptionHandler` uses the injected `Clock`; tests assert the fixed timestamp. |

### What is done well

* **Concurrency strategy is the strongest part.** The lock is per flight, so contention stays local. Availability is
  checked under the lock, and the database unique constraint is a real second line of defence. Cancel takes the lock
  *before* reading the booking, so it never acts on stale state. The concurrency test proves exactly one winner.
* **Seat history without a seats table.** Nulling `active_seat_label` on cancel frees the slot while keeping who sat
  where, and avoids pre-creating hundreds of thousands of seat rows.
* **Clear layering and transaction boundaries.** Controllers only translate HTTP; services own the transactions;
  open-session-in-view is off; read paths are `readOnly`.
* **Testable time.** An injectable UTC `Clock` and a pure, static `operatingDates()` make date logic unit-testable.
* **Uniform error contract** (`ApiError`) and Bean Validation on all request DTOs.
* **Tests:** 55 tests with 99.5% line coverage, a 90% gate in `mvn verify`, and seat counts read from data instead of
  hard-coded.

### Microservices and production-readiness recommendations

The assignment asks for a backend, and a **modular monolith is the right call** for this scope. If this were taken
to production as microservices, these are the next steps in priority order. They are not defects.

1. **Observability first:** add `spring-boot-starter-actuator` (liveness/readiness probes, metrics), a correlation
   id in the log MDC for every request, and structured JSON logs.
2. **Idempotent booking:** accept an `Idempotency-Key` header on `POST /bookings`, so a client retry after a
   timeout does not create a second booking.
3. **Service boundaries, if split:** *Schedule & Inventory* (schedules, instances, aircraft), *Search* (read model,
   cacheable, can scale independently) and *Booking* (owns seats and the lock). Publish `BookingCreated` and
   `BookingCancelled` events through a transactional **outbox** so Search's availability stays eventually
   consistent without distributed transactions.
4. **Hot-flight contention:** the per-flight lock serialises all bookings of one flight. At high load, move to
   per-seat holds with a short TTL, or rely on the unique constraint alone (optimistic insert, retry on 409).
5. **Database:** PostgreSQL (the Flyway migrations from **M3** carry over, apart from the H2-specific `MERGE … KEY`
   seed syntax), and Testcontainers in the integration tests so they run against the production engine instead of H2.
6. **API gateway** for authentication, an admin role for `/api/v1/admin/**`, and rate limiting on search.

### Review log

| Date | Reviewer | Summary |
|---|---|---|
| 2026-10-04 | Java Microservices Lead | Initial review against the assignment: all functional requirements met; 2 High, 5 Medium and 7 Low findings opened. H1 verified on the running application (404/405/415 returned as 500). |
| 2026-10-04 | Java Microservices Lead | Fix round: H1, H2, M1–M5, L1, L2, L3, L7 fixed; L4, L5, L6 partly fixed (remaining work noted per item). Verified with a plain `mvn clean verify` on JDK 26 (55 tests, 99.5% line coverage) and by smoke-testing the jar in the default and `prod` profiles. Verdict changed to **Approved**. |
