package com.airline.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.airline.TestFlights;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIT {
    @Autowired
    MockMvc mvc;

    @Test
    void fullFlow() throws Exception {
        String fn = TestFlights.uniqueFlightNumber("Z");
        mvc.perform(post("/api/v1/admin/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"flightNumber":"%s","sourceAirport":"DXB","destinationAirport":"LHR","departureTime":"09:30",
                 "arrivalTime":"13:45","aircraftId":1,
                 "daysOfOperation":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}
                """.formatted(fn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.instancesGenerated").isNumber());

        String date = LocalDate.now(ZoneOffset.UTC).plusDays(3).toString();
        String body = mvc.perform(get("/api/v1/flights/search")
                        .param("source", "DXB")
                        .param("destination", "LHR")
                        .param("date", date))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> ids = JsonPath.read(body, "$[?(@.flightNumber=='" + fn + "')].flightInstanceId");
        long id = ids.get(0).longValue();
        String seatMap = mvc.perform(get("/api/v1/flights/" + id + "/seats"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int capacity = JsonPath.read(seatMap, "$.totalSeats");

        String booking = """
                {"flightInstanceId":%d,"passengers":[{"name":"Ann","seat":"1A"},{"name":"Ben","seat":"1B"}]}""".formatted(id);
        String res = mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String pnr = JsonPath.read(res, "$.pnr");

        mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(booking))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/flights/" + id + "/seats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSeats").value(capacity - 2));
        mvc.perform(get("/api/v1/bookings/" + pnr)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/bookings/" + pnr + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/bookings/" + pnr + "/cancel")).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/flights/" + id + "/seats"))
                .andExpect(jsonPath("$.availableSeats").value(capacity));
    }

    @Test
    void validationErrors() throws Exception {
        mvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flightInstanceId\":1,\"passengers\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/flights/search")
                        .param("source", "DXB")
                        .param("destination", "LHR")
                        .param("date", "not-a-date"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bookings/does-not-exist")).andExpect(status().isNotFound());
    }
}
