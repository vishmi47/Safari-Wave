package com.example.safariwave_backend.repository;

import com.example.safariwave_backend.entity.SafariTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface SafariTripRepository extends JpaRepository<SafariTrip, Integer> {

    // Used by the package module to decide between hard delete and archiving
    boolean existsBySafariPackage_PackageId(Integer packageId);

    // Trips assigned to a guide
    List<SafariTrip> findByGuide_UserIdOrderByTripDateAsc(Integer guideId);

    boolean existsByBoat_BoatId(Integer boatId);

    // Upcoming trips that still need their boat (used before retiring a boat or lowering its capacity)
    List<SafariTrip> findByBoat_BoatIdAndTripDateGreaterThanEqualAndStatusIn(Integer boatId, LocalDate fromDate, List<String> statuses);
}
