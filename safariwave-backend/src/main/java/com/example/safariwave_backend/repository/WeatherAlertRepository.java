package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.WeatherAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Integer> {

    List<WeatherAlert> findBySeverity(String severity);

    List<WeatherAlert> findByAlertStatus(String alertStatus);
}
