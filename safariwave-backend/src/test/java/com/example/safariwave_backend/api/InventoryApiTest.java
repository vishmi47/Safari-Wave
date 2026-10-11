package com.example.safariwave_backend.api;

import com.example.safariwave_backend.entity.Boat;
import com.example.safariwave_backend.security.Roles;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inventory & Asset endpoints across roles (spec 6.2 / B.3). */
class InventoryApiTest extends ApiTestSupport {

    @Test
    void inventoryPermissions_matchTheMatrix() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        TestUser finance = createUser(Roles.FINANCE);
        TestUser customer = createUser(Roles.CUSTOMER);
        TestUser admin = createUser(Roles.ADMIN);

        // Operations adds a boat (previously only ADMIN could)
        String firstBoat = mockMvc.perform(as(ops, post("/api/boats")).content("""
                        {"boatName": "Egret", "registrationNumber": "WL-777", "capacity": 8, "status": "OPERATIONAL"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer firstBoatId = com.jayway.jsonpath.JsonPath.read(firstBoat, "$.boatId");
        mockMvc.perform(as(ops, post("/api/boats")).content("""
                        {"boatName": "Egret 2", "registrationNumber": "wl-777", "capacity": 8, "status": "OPERATIONAL"}"""))
                .andExpect(status().isConflict());

        // Read: operations, guide, finance yes; customer no
        mockMvc.perform(as(guide, get("/api/boats/search?q=egret"))).andExpect(jsonPath("$.page.totalElements").value(1));
        mockMvc.perform(as(finance, get("/api/boats"))).andExpect(status().isOk());
        mockMvc.perform(as(customer, get("/api/boats"))).andExpect(status().isForbidden());
        mockMvc.perform(as(customer, get("/api/assets"))).andExpect(status().isForbidden());

        // Assets: operations creates, guide reports condition, finance cannot edit
        String secondBoat = mockMvc.perform(as(ops, post("/api/boats")).content("""
                        {"boatName": "Egret spare", "registrationNumber": "WL-778", "capacity": 6, "status": "OPERATIONAL"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer secondBoatId = com.jayway.jsonpath.JsonPath.read(secondBoat, "$.boatId");
        String asset = mockMvc.perform(as(ops, post("/api/assets")).content("""
                        {"assetName": "Life jacket M", "assetType": "SAFETY_GEAR", "quantity": 20, "boatIds": [%d, %d]}""".formatted(firstBoatId, secondBoatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.boats.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Integer assetId = com.jayway.jsonpath.JsonPath.read(asset, "$.assetId");
        mockMvc.perform(as(admin, delete("/api/boats/" + firstBoatId + "?permanent=true")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("assets")));
        mockMvc.perform(as(ops, post("/api/assets")).content("{\"assetName\": \"\", \"assetType\": \"X\", \"quantity\": -1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(as(guide, patch("/api/assets/" + assetId + "/condition")).content("{\"status\": \"DAMAGED\", \"remarks\": \"torn\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionStatus").value("DAMAGED"));
        mockMvc.perform(as(finance, patch("/api/assets/" + assetId + "/condition")).content("{\"status\": \"GOOD\"}"))
                .andExpect(status().isForbidden());

        // Operations "delete" retires; only admin can remove permanently
        mockMvc.perform(as(ops, delete("/api/assets/" + assetId))).andExpect(status().isNoContent());
        mockMvc.perform(as(ops, get("/api/assets/" + assetId))).andExpect(jsonPath("$.status").value("RETIRED"));
        mockMvc.perform(as(ops, delete("/api/assets/" + assetId + "?permanent=true"))).andExpect(status().isForbidden());
        mockMvc.perform(as(admin, delete("/api/assets/" + assetId + "?permanent=true"))).andExpect(status().isNoContent());
    }

    @Test
    void boatWithUpcomingTrip_cannotBeRetired_andMaintenanceBlocksTrips() throws Exception {
        TestUser ops = createUser(Roles.OPERATIONS);
        TestUser guide = createUser(Roles.GUIDE);
        Boat boat = createBoat(10, "OPERATIONAL");
        Integer packageId = createPackage("2000.00", 10).getPackageId();
        mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "tripDate": "%s", "startTime": "08:00", "endTime": "10:00", "capacity": 10, "status": "SCHEDULED"}
                        """.formatted(packageId, boat.getBoatId(), LocalDate.now().plusDays(2))))
                .andExpect(status().isCreated());

        mockMvc.perform(as(ops, delete("/api/boats/" + boat.getBoatId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("upcoming trip")));

        // Guide reports an issue; operations puts the boat under maintenance; it can no longer take new trips
        mockMvc.perform(as(guide, post("/api/maintenance/issues")).content("{\"boatId\": %d, \"description\": \"Hull leak\"}".formatted(boat.getBoatId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maintenanceStatus").value("OPEN"));
        mockMvc.perform(as(ops, post("/api/maintenance")).content("""
                        {"boatId": %d, "inspectionDate": "%s", "maintenanceStatus": "UNDER_MAINTENANCE", "maintenanceCost": 15000}
                        """.formatted(boat.getBoatId(), LocalDate.now())))
                .andExpect(status().isCreated());
        mockMvc.perform(as(ops, post("/api/trips")).content("""
                        {"packageId": %d, "boatId": %d, "tripDate": "%s", "startTime": "13:00", "endTime": "15:00", "capacity": 5, "status": "SCHEDULED"}
                        """.formatted(packageId, boat.getBoatId(), LocalDate.now().plusDays(3))))
                .andExpect(status().isBadRequest());
    }
}
