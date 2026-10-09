package com.example.safariwave_backend.api;

import com.example.safariwave_backend.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Promotion & Safari Package endpoints across roles (spec 6.3 / B.3), including discounted booking prices. */
class PromotionApiTest extends ApiTestSupport {

    @Test
    void promotionLifecycle_andBookingCapturesDiscount() throws Exception {
        TestUser marketing = createUser(Roles.MARKETING);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser finance = createUser(Roles.FINANCE);
        TestUser ops = createUser(Roles.OPERATIONS);
        Integer packageId = createPackage("1000.00", 10).getPackageId();
        String body = """
                {"promotionName": "Monsoon Saver", "discountPercentage": 20, "startDate": "%s", "endDate": "%s", "packageId": %d}
                """.formatted(LocalDate.now().minusDays(1), LocalDate.now().plusDays(30), packageId);

        // Multipart create (the form the frontend sends) still works
        MockMultipartFile part = new MockMultipartFile("promotion", "", MediaType.APPLICATION_JSON_VALUE, body.getBytes());
        String created = mockMvc.perform(multipart("/api/promotions").file(part)
                        .header("Authorization", "Bearer " + marketing.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.packageName").exists())
                .andReturn().getResponse().getContentAsString();
        Integer promotionId = JsonPath.read(created, "$.promotionId");

        // Validation: discount over 100 and reversed dates
        mockMvc.perform(as(marketing, post("/api/promotions")).content(body.replace("\"discountPercentage\": 20", "\"discountPercentage\": 120")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(finance, post("/api/promotions")).content(body)).andExpect(status().isForbidden());

        // Drafts are invisible to customers; finance can read
        mockMvc.perform(as(customer, get("/api/promotions/" + promotionId))).andExpect(status().isNotFound());
        mockMvc.perform(as(finance, get("/api/promotions/search?status=DRAFT"))).andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(as(marketing, put("/api/promotions/" + promotionId + "/publish"))).andExpect(status().isOk());
        mockMvc.perform(as(customer, get("/api/promotions/search"))).andExpect(jsonPath("$.content[0].active").value(true));

        // A published, running promotion cannot be deleted by marketing
        mockMvc.perform(as(marketing, delete("/api/promotions/" + promotionId))).andExpect(status().isBadRequest());

        // A booking made now captures the 20% discount
        Integer boatId = createBoat(10, "OPERATIONAL").getBoatId();
        String trip = mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "tripDate": "%s", "startTime": "09:00", "endTime": "11:00", "capacity": 10, "status": "SCHEDULED"}
                        """.formatted(packageId, boatId, LocalDate.now().plusDays(5))))
                .andReturn().getResponse().getContentAsString();
        Integer tripId = JsonPath.read(trip, "$.tripId");
        mockMvc.perform(as(customer, post("/api/bookings")).content("{\"tripId\": %d, \"numberOfPeople\": 2}".formatted(tripId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountPercentage").value(20.0))
                .andExpect(jsonPath("$.totalPrice").value(1600.0));

        // Back to draft, then marketing can delete it
        mockMvc.perform(as(marketing, put("/api/promotions/" + promotionId + "/draft"))).andExpect(status().isOk());
        mockMvc.perform(as(marketing, delete("/api/promotions/" + promotionId))).andExpect(status().isNoContent());
    }

    @Test
    void packagesAreReadableByCustomers_butGuidesHaveNoAccess() throws Exception {
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser guide = createUser(Roles.GUIDE);
        createPackage("1200.00", 6);

        mockMvc.perform(as(customer, get("/api/packages"))).andExpect(status().isOk());
        mockMvc.perform(as(customer, get("/api/packages/search"))).andExpect(jsonPath("$.content").isArray());
        mockMvc.perform(as(guide, get("/api/packages"))).andExpect(status().isForbidden());
        mockMvc.perform(as(customer, post("/api/packages")).content("""
                        {"packageName": "Mine", "duration": 1, "basePrice": 10, "maxPassengers": 2, "status": "ACTIVE"}"""))
                .andExpect(status().isForbidden());
    }
}
