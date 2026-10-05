# Low-Level Design

## 1. Modules / packages (`com.airline`)
| Package | Responsibility |
|---|---|
| `config` | `AppConfig` (UTC `Clock`, OpenAPI metadata), `BookingWindow` (today..today+365 validation) |
| `domain` | JPA entities + enums: `Airport`, `Aircraft`, `FlightSchedule`, `FlightInstance`, `Booking`, `BookingSeat`, `BookingStatus`, `FlightStatus` |
| `repository` | Spring Data interfaces; custom JPQL for search, seat counting and the `FOR UPDATE` lock |
| `service` | Use cases and transaction boundaries |
| `web`, `web.dto` | REST controllers, request/response records, `ApiError` |
| `exception` | Domain exceptions + `GlobalExceptionHandler` |

## 2. Classes & services
* **Aircraft** - `capacity()`, `seatLabels()` (row-major `1A..30F`), `hasSeat(label)` (regex `^\d{1,2}[A-Z]$` + row/letter range).
* **FlightSchedule** - template; `daysOfOperation` is an element collection (`schedule_operating_day`).
* **FlightInstance** - one date of a schedule with `departure_utc/arrival_utc/status`.
* **Booking** - aggregate root: `addSeat`, `cancel(now)` (status + release seats), `@Version`.
* **BookingSeat** - `seat_label` (history) and `active_seat_label` (NULL when released).
* **ScheduleService.create** - validates, persists schedule, calls generator.
* **FlightInstanceGenerator** - static pure `operatingDates(days, from, to)`; `generate(schedule)`; `@Scheduled` nightly `extendWindow()`.
* **FlightSearchService.search**, **SeatMapService.get** - read-only.
* **BookingService** - `create`, `get`, `cancel`.

## 3. APIs
See README table. Base path `/api/v1`. JSON only. Booking request:
```json
{"flightInstanceId": 1, "passengers": [{"name": "Ann", "seat": "1A"}]}
```
Booking response: `pnr, flightNumber, flightDate, passengerCount, seats[], passengers[], status, createdAt, cancelledAt`.

## 4. Validation
| Layer | Rules |
|---|---|
| Bean Validation (DTO) | flight number 3-8 alnum; IATA codes 3 chars; times non-null; `arrivalDayOffset` 0..3; days non-empty; 1-9 passengers; names/seats non-blank |
| Service | airports/aircraft exist; src != dst; arrival after departure; unique flight number; date in window; flight open and not departed; seat exists on aircraft; no duplicate seats in request; seats free |
| Database | PK/FK/UNIQUE/CHECK constraints (final safety net) |

## 5. Error handling
`GlobalExceptionHandler` maps: `InvalidRequestException`/validation/type errors -> **400**, `ResourceNotFoundException` -> **404**,
`ConflictException`/`DataIntegrityViolation`/optimistic lock/lock timeout (`PessimisticLockingFailureException`) -> **409**.
Spring MVC's own errors (`ErrorResponse`: unknown URL, wrong method, unsupported media type) keep their status (**404/405/415**)
and headers. Anything else -> **500** (logged, generic message).
Body: `ApiError{timestamp,status,error,message,path,details}`, timestamp from the injected UTC `Clock`.

## 6. Transaction boundaries
| Operation | Boundary | Notes |
|---|---|---|
| create schedule | `ScheduleService.create` (single tx incl. instance generation) | all-or-nothing |
| create booking | `BookingService.create` | lock flight row -> validate -> insert; rollback on any exception |
| cancel booking | `BookingService.cancel` | lock flight row -> update booking/seats |
| search / seat map / get booking | `readOnly = true` | |
| nightly roll-forward | `FlightInstanceGenerator.extendWindow` | idempotent; one transaction per schedule, so a failure skips only that schedule |

Open-session-in-view is disabled; DTO mapping happens inside the transaction.

## 7. Sequence diagrams
`hld/diagrams/booking-sequence.*` and `hld/diagrams/cancel-sequence.*`.

## 8. Scalability notes
Per-flight lock keeps contention local; indexes on route and date; nightly job and generation are idempotent. To scale out,
replace H2 with PostgreSQL (row locks and unique constraints behave the same), and optionally move generation to a worker.
