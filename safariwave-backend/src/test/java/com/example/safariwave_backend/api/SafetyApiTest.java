package com.example.safariwave_backend.api;

import com.example.safariwave_backend.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Weather, Safety & Emergency endpoints across roles (spec 6.4 / B.3). */
class SafetyApiTest extends ApiTestSupport {

    @Test
    void weatherAlertPostponesTrip_andCustomerSeesAlertForTheirBooking() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        TestUser relations = createUser(Roles.CUSTOMER_RELATIONS);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser otherCustomer = createUser(Roles.CUSTOMER);
        LocalDate tripDay = LocalDate.now().plusDays(4);
        Integer packageId = createPackage("1000.00", 10).getPackageId();
        Integer boatId = createBoat(10, "OPERATIONAL").getBoatId();
        String trip = mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "tripDate": "%s", "startTime": "07:00", "endTime": "09:00", "capacity": 8, "status": "SCHEDULED"}
                        """.formatted(packageId, boatId, tripDay)))
                .andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(trip, "$.tripId");
        mockMvc.perform(as(customer, post("/api/bookings")).content("{\"tripId\": %d, \"numberOfPeople\": 2}".formatted(tripId)))
                .andExpect(status().isCreated());

        // Guides cannot record weather alerts; operations can
        String alertJson = """
                {"alertDateTime": "%s", "location": "Lagoon mouth", "weatherCondition": "Thunderstorm", "severity": "HIGH"}
                """.formatted(tripDay.atTime(6, 0));
        mockMvc.perform(as(guide, post("/api/weather-alerts")).content(alertJson)).andExpect(status().isForbidden());
        mockMvc.perform(as(ops, post("/api/weather-alerts")).content(alertJson.replace("HIGH", "EXTREME"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(ops, post("/api/weather-alerts")).content(alertJson)).andExpect(status().isCreated());

        // The customer with a booking that day sees it; another customer does not; relations reads all
        mockMvc.perform(as(customer, get("/api/weather-alerts"))).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(as(otherCustomer, get("/api/weather-alerts/search"))).andExpect(jsonPath("$.page.totalElements").value(0));
        mockMvc.perform(as(relations, get("/api/weather-alerts/search"))).andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(as(customer, get("/api/emergency-incidents"))).andExpect(status().isForbidden());

        // Operations postpones the trip because of the weather; the customer is notified
        mockMvc.perform(as(ops, patch("/api/trips/" + tripId + "/status")).content("{\"status\": \"SUSPENDED\", \"remarks\": \"Thunderstorm warning\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(as(customer, get("/api/notifications/user/" + customer.id())))
                .andExpect(jsonPath("$[0].message").value(org.hamcrest.Matchers.containsString("postponed")));
    }

    @Test
    void guideRecordsIncident_editsOwnOnly_andOnlyOperationsDeletes() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        TestUser otherGuide = createUser(Roles.GUIDE);
        String body = """
                {"incidentDateTime": "%s", "incidentType": "Passenger injury", "severity": "MEDIUM", "description": "Slipped on deck"}
                """.formatted(LocalDateTime.now().minusMinutes(30).withNano(0));
        String created = mockMvc.perform(as(guide, post("/api/emergency-incidents")).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        Integer incidentId = JsonPath.read(created, "$.incidentId");

        mockMvc.perform(as(otherGuide, put("/api/emergency-incidents/" + incidentId)).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(as(guide, put("/api/emergency-incidents/" + incidentId)).content(body.replace("MEDIUM", "LOW"))).andExpect(status().isOk());
        mockMvc.perform(as(guide, delete("/api/emergency-incidents/" + incidentId))).andExpect(status().isForbidden());
        mockMvc.perform(as(ops, delete("/api/emergency-incidents/" + incidentId))).andExpect(status().isNoContent());
    }
}
