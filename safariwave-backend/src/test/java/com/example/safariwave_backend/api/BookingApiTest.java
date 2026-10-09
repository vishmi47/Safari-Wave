package com.example.safariwave_backend.api;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.entity.SafariPackage;
import com.example.safariwave_backend.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end Booking & Reservation flow across roles (spec 6.1 / B.3). */
class BookingApiTest extends ApiTestSupport {

    @Test
    void unauthenticatedRequest_returns401Json() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void fullBookingFlow_respectsEveryRole() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser otherCustomer = createUser(Roles.CUSTOMER);
        TestUser finance = createUser(Roles.FINANCE);
        Boat boat = createBoat(6, "OPERATIONAL");
        SafariPackage safariPackage = createPackage("1500.00", 6);
        String tripDate = LocalDate.now().plusDays(7).toString();
        String tripJson = """
                {"packageId": %d, "boatId": %d, "guideId": %d, "tripDate": "%s",
                 "startTime": "09:00", "endTime": "11:00", "capacity": 5, "status": "SCHEDULED"}
                """.formatted(safariPackage.getPackageId(), boat.getBoatId(), guide.id(), tripDate);

        // A customer cannot schedule trips; the Operations Supervisor can
        mockMvc.perform(as(customer, post("/api/trips")).content(tripJson)).andExpect(status().isForbidden());
        String tripBody = mockMvc.perform(as(ops, post("/api/trips")).content(tripJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guideName").exists())
                .andExpect(jsonPath("$.availableSeats").value(5))
                .andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(tripBody, "$.tripId");

        // Customer books 4 of 5 seats; another customer cannot take 2 more
        String bookingBody = mockMvc.perform(as(customer, post("/api/bookings"))
                        .content("{\"tripId\": %d, \"numberOfPeople\": 4}".formatted(tripId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(6000.0))
                .andExpect(jsonPath("$.userId").value(customer.id()))
                .andReturn().getResponse().getContentAsString();
        Integer bookingId = JsonPath.read(bookingBody, "$.bookingId");
        mockMvc.perform(as(otherCustomer, post("/api/bookings"))
                        .content("{\"tripId\": %d, \"numberOfPeople\": 2}".formatted(tripId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only 1 seat(s) left on this trip."));

        // Customers only see their own bookings
        mockMvc.perform(as(otherCustomer, get("/api/bookings/" + bookingId))).andExpect(status().isForbidden());
        mockMvc.perform(as(otherCustomer, get("/api/bookings/search"))).andExpect(jsonPath("$.page.totalElements").value(0));
        mockMvc.perform(as(customer, get("/api/bookings/search"))).andExpect(jsonPath("$.content[0].bookingId").value(bookingId));

        // The assigned guide sees bookings on their trip
        mockMvc.perform(as(guide, get("/api/bookings"))).andExpect(jsonPath("$[0].bookingId").value(bookingId));

        // Finance changes payment status only
        mockMvc.perform(as(finance, put("/api/bookings/" + bookingId))
                        .content("{\"tripId\": %d, \"numberOfPeople\": 1}".formatted(tripId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(finance, patch("/api/bookings/" + bookingId + "/payment-status")).content("{\"status\": \"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));

        // The guide completes the trip; the confirmed booking becomes COMPLETED
        mockMvc.perform(as(guide, patch("/api/trips/" + tripId + "/status")).content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(as(customer, get("/api/bookings/" + bookingId)))
                .andExpect(jsonPath("$.bookingStatus").value("COMPLETED"));
    }

    @Test
    void cancellingTrip_cancelsBookings_andHardDeleteIsAdminOnly() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser relations = createUser(Roles.CUSTOMER_RELATIONS);
        Boat boat = createBoat(10, "OPERATIONAL");
        SafariPackage safariPackage = createPackage("1000.00", 10);
        String tripBody = mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "tripDate": "%s", "startTime": "08:00", "endTime": "10:00", "capacity": 10, "status": "SCHEDULED"}
                        """.formatted(safariPackage.getPackageId(), boat.getBoatId(), LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(tripBody, "$.tripId");

        // Customer relations books on behalf of the customer
        String bookingBody = mockMvc.perform(as(relations, post("/api/bookings"))
                        .content("{\"tripId\": %d, \"numberOfPeople\": 2, \"userId\": %d}".formatted(tripId, customer.id())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer bookingId = JsonPath.read(bookingBody, "$.bookingId");

        mockMvc.perform(as(relations, delete("/api/bookings/" + bookingId))).andExpect(status().isForbidden());

        mockMvc.perform(as(ops, post("/api/trips/" + tripId + "/cancel")).content("{\"remarks\": \"Storm warning\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(as(customer, get("/api/bookings/" + bookingId)))
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));
    }
}
