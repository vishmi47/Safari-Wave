package com.example.safariwave_backend.api;

import com.example.safariwave_backend.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Customer Experience endpoints across roles (spec 6.5 / B.3). */
class CustomerExperienceApiTest extends ApiTestSupport {

    @Test
    void complaintAndRatingLifecycle() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser relations = createUser(Roles.CUSTOMER_RELATIONS);
        TestUser marketing = createUser(Roles.MARKETING);
        TestUser finance = createUser(Roles.FINANCE);
        Integer packageId = createPackage("1000.00", 10).getPackageId();
        Integer boatId = createBoat(10, "OPERATIONAL").getBoatId();
        String trip = mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "guideId": %d, "tripDate": "%s", "startTime": "09:00", "endTime": "11:00", "capacity": 8, "status": "SCHEDULED"}
                        """.formatted(packageId, boatId, guide.id(), LocalDate.now().plusDays(2))))
                .andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(trip, "$.tripId");
        String booking = mockMvc.perform(as(customer, post("/api/bookings")).content("{\"tripId\": %d, \"numberOfPeople\": 2}".formatted(tripId)))
                .andReturn().getResponse().getContentAsString();
        Integer bookingId = JsonPath.read(booking, "$.bookingId");
        mockMvc.perform(as(customer, post("/api/payments/bookings/" + bookingId + "/mock")).content("{\"outcome\": \"SUCCESS\"}"))
                .andExpect(status().isOk());

        String ratingBody = """
                {"experienceType": "RATING", "subject": "Great trip", "description": "Saw kingfishers", "bookingId": %d, "rating": 5}
                """.formatted(bookingId);
        // Not completed yet: cannot rate
        mockMvc.perform(as(customer, post("/api/customer-experiences")).content(ratingBody)).andExpect(status().isBadRequest());

        // Customer files a complaint; customer relations cannot create one
        String complaint = mockMvc.perform(as(customer, post("/api/customer-experiences")).content("""
                        {"experienceType": "COMPLAINT", "subject": "Late start", "description": "Boat left 30 minutes late", "bookingId": %d}
                        """.formatted(bookingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        Integer complaintId = JsonPath.read(complaint, "$.id");
        mockMvc.perform(as(relations, post("/api/customer-experiences")).content(ratingBody)).andExpect(status().isForbidden());

        // Only customer relations responds; afterwards the customer can no longer edit it
        mockMvc.perform(as(customer, put("/api/customer-experiences/complaints/" + complaintId + "/response")).content("{\"response\": \"x\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(relations, put("/api/customer-experiences/complaints/" + complaintId + "/response"))
                        .content("{\"response\": \"Sorry, we have added a free drink voucher.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESPONDED"));
        mockMvc.perform(as(customer, delete("/api/customer-experiences/" + complaintId))).andExpect(status().isBadRequest());

        // The guide completes the trip; now the customer can rate it
        mockMvc.perform(as(guide, patch("/api/trips/" + tripId + "/status")).content("{\"status\": \"COMPLETED\"}")).andExpect(status().isOk());
        mockMvc.perform(as(customer, post("/api/customer-experiences")).content(ratingBody)).andExpect(status().isCreated());
        mockMvc.perform(as(customer, post("/api/customer-experiences")).content(ratingBody.replace("\"rating\": 5", "\"rating\": 9")))
                .andExpect(status().isBadRequest());

        // Marketing: ratings and stats only; guide: feedback on own trips; finance: nothing
        mockMvc.perform(as(marketing, get("/api/customer-experiences/ratings/summary"))).andExpect(jsonPath("$.averageRating").value(5.0));
        mockMvc.perform(as(marketing, get("/api/customer-experiences/search"))).andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(as(marketing, get("/api/customer-experiences/complaints"))).andExpect(status().isForbidden());
        mockMvc.perform(as(guide, get("/api/customer-experiences/my-trips"))).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(as(finance, get("/api/customer-experiences/search"))).andExpect(status().isForbidden());

        // Loyalty: customers enrol themselves; points are for customer relations
        mockMvc.perform(as(customer, post("/api/loyalty/customers/" + customer.id() + "/enroll"))).andExpect(status().isOk());
        mockMvc.perform(as(customer, post("/api/loyalty/customers/" + customer.id() + "/points?value=500"))).andExpect(status().isForbidden());
        mockMvc.perform(as(relations, post("/api/loyalty/customers/" + customer.id() + "/points?value=500")))
                .andExpect(jsonPath("$.points").value(500));
    }
}
