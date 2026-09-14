package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.entity.Vendor;
import com.example.safariwave_backend.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@CrossOrigin(origins = "*") // Allows frontend connection from any origin
public class VendorController {

    @Autowired
    private VendorService vendorService;

    /**
     * POST /api/vendors/register
     * Public endpoint for vendor registration
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Vendor vendor) {
        try {
            return ResponseEntity.ok(vendorService.registerVendor(vendor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * GET /api/vendors
     * Admin/GM: View all vendors
     */
    @GetMapping
    public ResponseEntity<List<Vendor>> getAll() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    /**
     * GET /api/vendors/pending
     * Admin/GM: View only pending approvals
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Vendor>> getPending() {
        return ResponseEntity.ok(vendorService.getPendingVendors());
    }

    /**
     * PUT /api/vendors/{id}/approve
     * Admin/GM: Approve a vendor
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.approveVendor(id));
    }

    /**
     * PUT /api/vendors/{id}/reject
     * Admin/GM: Reject a vendor
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.rejectVendor(id));
    }

    /**
     * PUT /api/vendors/{id}/deactivate
     * Admin/GM: Deactivate a vendor
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.deactivateVendor(id));
    }
}