package com.example.safariwave_backend.service;

import com.example.safariwave_backend.dto.request.WeatherAlertRequestDTO;
import com.example.safariwave_backend.dto.response.WeatherAlertResponseDTO;

import java.util.List;
import java.util.Optional;

public interface WeatherAlertService {

    WeatherAlertResponseDTO createWeatherAlert(WeatherAlertRequestDTO dto);

    Optional<WeatherAlertResponseDTO> getWeatherAlertById(Integer alertId);

    List<WeatherAlertResponseDTO> getAllWeatherAlerts();

    List<WeatherAlertResponseDTO> getWeatherAlertsBySeverity(String severity);

    List<WeatherAlertResponseDTO> getWeatherAlertsByStatus(String status);

    WeatherAlertResponseDTO updateWeatherAlert(Integer alertId, WeatherAlertRequestDTO dto);

    void deleteWeatherAlert(Integer alertId);
}
