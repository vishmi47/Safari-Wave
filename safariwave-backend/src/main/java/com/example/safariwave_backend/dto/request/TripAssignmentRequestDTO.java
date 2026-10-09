package com.example.safariwave_backend.dto.request;

/** Operations Supervisor assigns the boat and/or guide of a trip. Null fields are left unchanged. */
public class TripAssignmentRequestDTO {

    private Integer boatId;

    private Integer guideId;

    public TripAssignmentRequestDTO() {
    }

    public Integer getBoatId() {
        return boatId;
    }

    public void setBoatId(Integer boatId) {
        this.boatId = boatId;
    }

    public Integer getGuideId() {
        return guideId;
    }

    public void setGuideId(Integer guideId) {
        this.guideId = guideId;
    }
}
