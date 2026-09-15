package com.example.safariwave_backend.service.impl;

import com.example.safariwave_backend.dto.request.WeatherAlertRequestDTO;
import com.example.safariwave_backend.dto.response.WeatherAlertResponseDTO;
import com.example.safariwave_backend.entity.WeatherAlert;
import com.example.safariwave_backend.repository.WeatherAlertRepository;
import com.example.safariwave_backend.service.WeatherAlertService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WeatherAlertServiceImpl implements WeatherAlertService {

    private final WeatherAlertRepository weatherAlertRepository;

    public WeatherAlertServiceImpl(WeatherAlertRepository weatherAlertRepository) {
        this.weatherAlertRepository = weatherAlertRepository;
    }

    @Override
    public WeatherAlertResponseDTO createWeatherAlert(WeatherAlertRequestDTO dto) {
        if (dto == null || dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            throw new RuntimeException("Location is required.");
        }

        WeatherAlert alert = new WeatherAlert();
        alert.setAlertDateTime(dto.getAlertDateTime());
        alert.setLocation(dto.getLocation());
        alert.setWeatherCondition(dto.getWeatherCondition());
        alert.setSeverity(dto.getSeverity());
        alert.setDescription(dto.getDescription());
        alert.setAlertStatus(dto.getAlertStatus() == null ? "ACTIVE" : dto.getAlertStatus().toUpperCase());
        alert.setRemarks(dto.getRemarks());

        WeatherAlert saved = weatherAlertRepository.save(alert);
        return toResponseDTO(saved);
    }

    @Override
    public Optional<WeatherAlertResponseDTO> getWeatherAlertById(Integer alertId) {
        return weatherAlertRepository.findById(alertId).map(this::toResponseDTO);
    }

    @Override
    public List<WeatherAlertResponseDTO> getAllWeatherAlerts() {
        return weatherAlertRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<WeatherAlertResponseDTO> getWeatherAlertsBySeverity(String severity) {
        return weatherAlertRepository.findBySeverity(severity).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<WeatherAlertResponseDTO> getWeatherAlertsByStatus(String status) {
        return weatherAlertRepository.findByAlertStatus(status.toUpperCase()).stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public WeatherAlertResponseDTO updateWeatherAlert(Integer alertId, WeatherAlertRequestDTO dto) {
        WeatherAlert existing = weatherAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Weather alert not found with id: " + alertId));

        existing.setAlertDateTime(dto.getAlertDateTime());
        existing.setLocation(dto.getLocation());
        existing.setWeatherCondition(dto.getWeatherCondition());
        existing.setSeverity(dto.getSeverity());
        existing.setDescription(dto.getDescription());
        existing.setAlertStatus(dto.getAlertStatus() == null ? existing.getAlertStatus() : dto.getAlertStatus().toUpperCase());
        existing.setRemarks(dto.getRemarks());

        WeatherAlert updated = weatherAlertRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deleteWeatherAlert(Integer alertId) {
        if (!weatherAlertRepository.existsById(alertId)) {
            throw new RuntimeException("Weather alert not found with id: " + alertId);
        }
        weatherAlertRepository.deleteById(alertId);
    }

    private WeatherAlertResponseDTO toResponseDTO(WeatherAlert alert) {
        WeatherAlertResponseDTO dto = new WeatherAlertResponseDTO();
        dto.setAlertId(alert.getAlertId());
        dto.setAlertDateTime(alert.getAlertDateTime());
        dto.setLocation(alert.getLocation());
        dto.setWeatherCondition(alert.getWeatherCondition());
        dto.setSeverity(alert.getSeverity());
        dto.setDescription(alert.getDescription());
        dto.setAlertStatus(alert.getAlertStatus());
        dto.setRemarks(alert.getRemarks());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setUpdatedAt(alert.getUpdatedAt());
        return dto;
    }
}
