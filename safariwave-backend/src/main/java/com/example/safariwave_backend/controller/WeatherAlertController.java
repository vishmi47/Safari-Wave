package com.example.safariwave_backend.controller;

import com.example.safariwave_backend.dto.request.WeatherAlertRequestDTO;
import com.example.safariwave_backend.dto.response.WeatherAlertResponseDTO;
import com.example.safariwave_backend.service.WeatherAlertService;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

// Role rules are enforced in WeatherAlertServiceImpl (see SafetyPermissions).
@RestController
@RequestMapping("/api/weather-alerts")
public class WeatherAlertController {

    private final WeatherAlertService weatherAlertService;

    public WeatherAlertController(WeatherAlertService weatherAlertService) {
        this.weatherAlertService = weatherAlertService;
    }

    @PostMapping
    public ResponseEntity<WeatherAlertResponseDTO> createWeatherAlert(@Valid @RequestBody WeatherAlertRequestDTO dto) {
        WeatherAlertResponseDTO saved = weatherAlertService.createWeatherAlert(dto);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<WeatherAlertResponseDTO>> getAllWeatherAlerts() {
        return ResponseEntity.ok(weatherAlertService.getAllWeatherAlerts());
    }

    // Paged list with search: GET /api/weather-alerts/search?q=&status=&page=0&size=10
    @GetMapping("/search")
    public ResponseEntity<PagedModel<WeatherAlertResponseDTO>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(new PagedModel<>(weatherAlertService.searchWeatherAlerts(q, status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WeatherAlertResponseDTO> getWeatherAlertById(@PathVariable Integer id) {
        return weatherAlertService.getWeatherAlertById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<WeatherAlertResponseDTO>> getWeatherAlertsBySeverity(@PathVariable String severity) {
        return ResponseEntity.ok(weatherAlertService.getWeatherAlertsBySeverity(severity));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<WeatherAlertResponseDTO>> getWeatherAlertsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(weatherAlertService.getWeatherAlertsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WeatherAlertResponseDTO> updateWeatherAlert(@PathVariable Integer id, @Valid @RequestBody WeatherAlertRequestDTO dto) {
        WeatherAlertResponseDTO updated = weatherAlertService.updateWeatherAlert(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWeatherAlert(@PathVariable Integer id) {
        weatherAlertService.deleteWeatherAlert(id);
        return ResponseEntity.noContent().build();
    }
}
