package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.entity.Vendor;
import com.example.safariwave_backend.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@CrossOrigin(origins = "*") // Frontend එක connect වෙන්න allow කරයි
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Vendor vendor) {
        try {
            return ResponseEntity.ok(vendorService.registerVendor(vendor));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Vendor>> getAll() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Vendor>> getPending() {
        return ResponseEntity.ok(vendorService.getPendingVendors());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.approveVendor(id));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.rejectVendor(id));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.deactivateVendor(id));
    }
}