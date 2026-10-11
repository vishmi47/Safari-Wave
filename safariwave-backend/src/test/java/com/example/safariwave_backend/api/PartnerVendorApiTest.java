package com.example.safariwave_backend.api;

import com.example.safariwave_backend.security.Roles;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Partner & Vendor endpoints across roles (spec 6.6 / B.3). */
class PartnerVendorApiTest extends ApiTestSupport {

    private String partner(String name, String type, String email, String documentUrl) {
        return """
                {"businessName": "%s", "partnerType": "%s", "contactPerson": "Dinesh", "email": "%s", "phone": "+94771234567",
                 "serviceType": "Services", "contractStartDate": "%s", "contractEndDate": "%s", "documentUrl": "%s"}
                """.formatted(name, type, email, LocalDate.now(), LocalDate.now().plusMonths(12), documentUrl);
    }

    @Test
    void partnerVisibilityAndLifecycle_followTheMatrix() throws Exception {
        TestUser admin = createUser(Roles.ADMIN);
        TestUser finance = createUser(Roles.FINANCE);
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser marketing = createUser(Roles.MARKETING);
        TestUser customer = createUser(Roles.CUSTOMER);

        // The agreement document is really stored and is private
        MockMultipartFile pdf = new MockMultipartFile("file", "contract.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        String documentUrl = mockMvc.perform(multipart("/api/partners-vendors/upload-document").file(pdf)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String hotel = mockMvc.perform(as(admin, post("/api/partners-vendors")).content(partner("Lagoon Resort", "Hotel", "resort@test.lk", documentUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.partnerType").value("HOTEL"))
                .andReturn().getResponse().getContentAsString();
        Integer hotelId = JsonPath.read(hotel, "$.partnerVendorId");
        mockMvc.perform(as(admin, post("/api/partners-vendors")).content(partner("Jet Engines Ltd", "EQUIPMENT_SUPPLIER", "jet@test.lk", "/x.pdf")))
                .andExpect(status().isCreated());
        mockMvc.perform(as(admin, post("/api/partners-vendors")).content(partner("Bad", "SPACESHIP", "bad@test.lk", "/x.pdf")))
                .andExpect(status().isBadRequest());

        // Only admin writes
        mockMvc.perform(as(finance, post("/api/partners-vendors")).content(partner("F", "HOTEL", "f@test.lk", "/x.pdf"))).andExpect(status().isForbidden());

        // Finance sees all, operations only suppliers/transport, marketing only hotels/agencies, customers nothing
        mockMvc.perform(as(finance, get("/api/partners-vendors"))).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(as(ops, get("/api/partners-vendors/search"))).andExpect(jsonPath("$.content[0].businessName").value("Jet Engines Ltd"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(as(marketing, get("/api/partners-vendors"))).andExpect(jsonPath("$[0].businessName").value("Lagoon Resort"));
        mockMvc.perform(as(ops, get("/api/partners-vendors/" + hotelId))).andExpect(status().isForbidden());
        mockMvc.perform(as(customer, get("/api/partners-vendors"))).andExpect(status().isForbidden());

        // Document download follows the same visibility
        String filename = documentUrl.substring(documentUrl.lastIndexOf('/') + 1);
        mockMvc.perform(as(marketing, get("/api/partners-vendors/documents/" + filename)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mockMvc.perform(as(ops, get("/api/partners-vendors/documents/" + filename))).andExpect(status().isForbidden());

        // Delete is a soft delete first; permanent delete only once inactive or terminated
        mockMvc.perform(as(admin, delete("/api/partners-vendors/" + hotelId + "?permanent=true"))).andExpect(status().isBadRequest());
        mockMvc.perform(as(admin, put("/api/partners-vendors/" + hotelId + "/terminate"))).andExpect(jsonPath("$.status").value("TERMINATED"));
        mockMvc.perform(as(admin, delete("/api/partners-vendors/" + hotelId + "?permanent=true"))).andExpect(status().isNoContent());
    }

    @Test
    void vendorCanHaveMultipleAgreements_andAgreementsHaveAdminCrud() throws Exception {
        TestUser admin = createUser(Roles.ADMIN);
        TestUser finance = createUser(Roles.FINANCE);
        TestUser ops = createUser(Roles.OPERATIONS);
        String vendor = mockMvc.perform(as(admin, post("/api/partners-vendors"))
                        .content(partner("Agreement Hotel", "HOTEL", "agreement@test.lk", "/legacy.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agreements.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        Integer vendorId = JsonPath.read(vendor, "$.partnerVendorId");
        Integer originalAgreementId = JsonPath.read(vendor, "$.agreements[0].agreementId");
        String agreementBody = """
                {"startDate":"%s","endDate":"%s","terms":"Room allocation and seasonal rates"}
                """.formatted(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(6));

        String additional = mockMvc.perform(as(admin, post("/api/partners-vendors/" + vendorId + "/agreements"))
                        .content(agreementBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.terms").value("Room allocation and seasonal rates"))
                .andReturn().getResponse().getContentAsString();
        Integer agreementId = JsonPath.read(additional, "$.agreementId");

        mockMvc.perform(as(finance, get("/api/partners-vendors/" + vendorId + "/agreements")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(as(ops, get("/api/partners-vendors/" + vendorId + "/agreements")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(finance, post("/api/partners-vendors/" + vendorId + "/agreements"))
                        .content(agreementBody))
                .andExpect(status().isForbidden());

        String updatedAgreement = """
                {"startDate":"%s","endDate":"%s","terms":"Updated seasonal rates"}
                """.formatted(LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(8));
        mockMvc.perform(as(admin, put("/api/partners-vendors/" + vendorId + "/agreements/" + agreementId))
                        .content(updatedAgreement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms").value("Updated seasonal rates"));
        mockMvc.perform(as(admin, delete("/api/partners-vendors/" + vendorId + "/agreements/" + agreementId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(admin, get("/api/partners-vendors/" + vendorId + "/agreements")))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(as(admin, delete("/api/partners-vendors/" + vendorId + "/agreements/" + originalAgreementId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(as(admin, get("/api/partners-vendors/" + vendorId + "/agreements")))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
