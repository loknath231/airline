package com.airline.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airline.TestFlights;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Every error branch reachable over HTTP, plus the read-only helper endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiErrorHandlingIT {
    private static final String TOMORROW =
            LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

    @Autowired
    MockMvc mvc;

    private ResultActions createSchedule(
            String flightNumber, String src, String dst, String dep, String arr, Integer offset, long aircraftId)
            throws Exception {
        String json = """
                {"flightNumber":"%s","sourceAirport":"%s","destinationAirport":"%s","departureTime":"%s",
                 "arrivalTime":"%s","arrivalDayOffset":%s,"aircraftId":%d,"daysOfOperation":["MONDAY"]}""".formatted(flightNumber, src, dst, dep, arr, offset, aircraftId);
        return mvc.perform(post("/api/v1/admin/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions search(String src, String dst, String date) throws Exception {
        return mvc.perform(get("/api/v1/flights/search")
                .param("source", src)
                .param("destination", dst)
                .param("date", date));
    }

    // ---- schedules -------------------------------------------------------------------------

    @Test
    void schedule_overnightWithDayOffset_isCreated() throws Exception {
        createSchedule(TestFlights.uniqueFlightNumber("N"), "dxb", "lhr", "22:00", "02:00", 1, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceAirport").value("DXB"))
                .andExpect(jsonPath("$.arrivalDayOffset").value(1));
    }

    @Test
    void schedule_duplicateFlightNumber_is409() throws Exception {
        String fn = TestFlights.uniqueFlightNumber("D");
        createSchedule(fn, "DXB", "LHR", "09:00", "13:00", 0, 1).andExpect(status().isCreated());
        createSchedule(fn.toLowerCase(), "DXB", "LHR", "09:00", "13:00", 0, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Flight number already exists: " + fn));
    }

    @Test
    void schedule_unknownAirport_is404() throws Exception {
        createSchedule(TestFlights.uniqueFlightNumber("U"), "XXX", "LHR", "09:00", "13:00", 0, 1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Airport not found: XXX"));
    }

    @Test
    void schedule_sameSourceAndDestination_is400() throws Exception {
        createSchedule(TestFlights.uniqueFlightNumber("S"), "DXB", "DXB", "09:00", "13:00", 0, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source and destination airports must differ"));
    }

    @Test
    void schedule_unknownAircraft_is404() throws Exception {
        createSchedule(TestFlights.uniqueFlightNumber("A"), "DXB", "LHR", "09:00", "13:00", 0, 99999)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aircraft not found: 99999"));
    }

    @Test
    void schedule_arrivalNotAfterDeparture_is400() throws Exception {
        createSchedule(TestFlights.uniqueFlightNumber("R"), "DXB", "LHR", "09:00", "09:00", 0, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Arrival must be after departure")));
    }

    @Test
    void schedule_beanValidation_returnsFieldDetails() throws Exception {
        createSchedule("!", "DXB", "LHR", "09:00", "13:00", 0, 1)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").value(hasItem("flightNumber: must be 3-8 alphanumeric characters")));
    }

    @Test
    void schedule_createReturnsLocation_andCanBeReadBackAndListed() throws Exception {
        String fn = TestFlights.uniqueFlightNumber("L");
        String location = createSchedule(fn, "DXB", "LHR", "09:00", "13:00", 0, 1)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/admin/schedules/\\d+")))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flightNumber").value(fn))
                .andExpect(jsonPath("$.aircraftType").exists());
        mvc.perform(get("/api/v1/admin/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].flightNumber").value(hasItem(fn)));
    }

    @Test
    void schedule_unknownId_is404() throws Exception {
        mvc.perform(get("/api/v1/admin/schedules/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Schedule not found: 999999"));
    }

    // ---- search & seat map -----------------------------------------------------------------

    @Test
    void search_missingParameter_is400() throws Exception {
        mvc.perform(get("/api/v1/flights/search").param("source", "DXB").param("date", TOMORROW))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing parameter 'destination'"));
    }

    @Test
    void search_blankSource_is400() throws Exception {
        search(" ", "LHR", TOMORROW)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("source, destination and date are required"));
    }

    @Test
    void search_sameSourceAndDestination_is400() throws Exception {
        search("DXB", "dxb", TOMORROW)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source and destination must differ"));
    }

    @Test
    void search_dateOutsideWindow_is400() throws Exception {
        search("DXB", "LHR", LocalDate.now(ZoneOffset.UTC).minusDays(1).toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Date must be between")));
    }

    @Test
    void search_unknownAirport_is404() throws Exception {
        search("DXB", "ZZZ", TOMORROW)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Airport not found: ZZZ"));
    }

    @Test
    void search_routeWithoutFlights_isEmpty() throws Exception {
        search("GIG", "GRU", TOMORROW)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void seatMap_unknownFlight_is404() throws Exception {
        mvc.perform(get("/api/v1/flights/999999/seats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flight not found: 999999"));
    }

    @Test
    void seatMap_nonNumericId_is400() throws Exception {
        mvc.perform(get("/api/v1/flights/abc/seats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'flightInstanceId'"));
    }

    // ---- bookings --------------------------------------------------------------------------

    @Test
    void booking_malformedJson_is400() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    @Test
    void booking_unknownFlight_is404() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightInstanceId\":999999,\"passengers\":[{\"name\":\"Ann\",\"seat\":\"1A\"}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Flight not found: 999999"));
    }

    @Test
    void cancel_unknownPnr_is404() throws Exception {
        mvc.perform(post("/api/v1/bookings/no-such-pnr/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking not found: no-such-pnr"));
    }

    // ---- reference data --------------------------------------------------------------------

    @Test
    void airports_listsSeededAirports() throws Exception {
        mvc.perform(get("/api/v1/airports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code").value(hasItem("DXB")))
                .andExpect(jsonPath("$[?(@.code=='DXB')].city").value(hasItem("Dubai")));
    }

    // ---- framework errors keep their real status (not 500) ----------------------------------

    @Test
    void unknownUrl_is404() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/v1/does-not-exist"));
    }

    @Test
    void wrongMethod_is405_withAllowHeader() throws Exception {
        mvc.perform(delete("/api/v1/airports"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unsupportedContentType_is415() throws Exception {
        mvc.perform(post("/api/v1/bookings").contentType(MediaType.TEXT_PLAIN).content("hi"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }
}
