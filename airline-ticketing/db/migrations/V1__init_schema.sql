-- Airline ticketing schema (H2). All timestamps are stored in UTC.
CREATE TABLE IF NOT EXISTS airport (
    code    VARCHAR(3)   PRIMARY KEY,
    name    VARCHAR(150) NOT NULL,
    city    VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS aircraft (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    aircraft_type VARCHAR(50) NOT NULL,
    total_rows    INT         NOT NULL CHECK (total_rows > 0),
    seat_letters  VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS flight_schedule (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    flight_number       VARCHAR(10) NOT NULL,
    source_airport      VARCHAR(3)  NOT NULL,
    destination_airport VARCHAR(3)  NOT NULL,
    departure_time      TIME        NOT NULL,
    arrival_time        TIME        NOT NULL,
    arrival_day_offset  INT         NOT NULL DEFAULT 0 CHECK (arrival_day_offset BETWEEN 0 AND 3),
    aircraft_id         BIGINT      NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_schedule_flight_number UNIQUE (flight_number),
    CONSTRAINT fk_schedule_source   FOREIGN KEY (source_airport)      REFERENCES airport (code),
    CONSTRAINT fk_schedule_dest     FOREIGN KEY (destination_airport) REFERENCES airport (code),
    CONSTRAINT fk_schedule_aircraft FOREIGN KEY (aircraft_id)         REFERENCES aircraft (id),
    CONSTRAINT ck_schedule_route CHECK (source_airport <> destination_airport)
);
CREATE INDEX IF NOT EXISTS idx_schedule_route ON flight_schedule (source_airport, destination_airport);

CREATE TABLE IF NOT EXISTS schedule_operating_day (
    schedule_id BIGINT      NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (schedule_id, day_of_week),
    CONSTRAINT fk_opday_schedule FOREIGN KEY (schedule_id) REFERENCES flight_schedule (id)
);

CREATE TABLE IF NOT EXISTS flight_instance (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    schedule_id   BIGINT      NOT NULL,
    flight_date   DATE        NOT NULL,
    departure_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    arrival_utc   TIMESTAMP WITH TIME ZONE NOT NULL,
    status        VARCHAR(15) NOT NULL DEFAULT 'SCHEDULED',
    CONSTRAINT uq_instance_schedule_date UNIQUE (schedule_id, flight_date),
    CONSTRAINT fk_instance_schedule FOREIGN KEY (schedule_id) REFERENCES flight_schedule (id)
);
CREATE INDEX IF NOT EXISTS idx_instance_date ON flight_instance (flight_date);

CREATE TABLE IF NOT EXISTS booking (
    id                 BIGINT      AUTO_INCREMENT PRIMARY KEY,
    pnr                VARCHAR(36) NOT NULL,
    flight_instance_id BIGINT      NOT NULL,
    status             VARCHAR(15) NOT NULL,
    passenger_count    INT         NOT NULL CHECK (passenger_count > 0),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    cancelled_at       TIMESTAMP WITH TIME ZONE,
    version            BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_booking_pnr UNIQUE (pnr),
    CONSTRAINT fk_booking_instance FOREIGN KEY (flight_instance_id) REFERENCES flight_instance (id)
);
CREATE INDEX IF NOT EXISTS idx_booking_instance ON booking (flight_instance_id);

-- One row per passenger/seat. active_seat_label is set while the booking holds the seat and
-- becomes NULL on cancellation. UNIQUE(flight_instance_id, active_seat_label) makes double
-- booking impossible at DB level while keeping cancelled history (NULLs are distinct in UNIQUE).
CREATE TABLE IF NOT EXISTS booking_seat (
    id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
    booking_id         BIGINT       NOT NULL,
    flight_instance_id BIGINT       NOT NULL,
    seat_label         VARCHAR(4)   NOT NULL,
    passenger_name     VARCHAR(100) NOT NULL,
    active_seat_label  VARCHAR(4),
    CONSTRAINT uq_active_seat UNIQUE (flight_instance_id, active_seat_label),
    CONSTRAINT fk_bseat_booking  FOREIGN KEY (booking_id)         REFERENCES booking (id),
    CONSTRAINT fk_bseat_instance FOREIGN KEY (flight_instance_id) REFERENCES flight_instance (id)
);
CREATE INDEX IF NOT EXISTS idx_bseat_booking ON booking_seat (booking_id);
