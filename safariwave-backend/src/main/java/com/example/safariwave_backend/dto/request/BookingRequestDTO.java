package com.example.safariwave_backend.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.time.LocalDate;

public class BookingRequestDTO {

    // Customers always book for themselves; staff booking on a customer's behalf must send userId.
    private Integer userId;
    @Valid
    private WalkInCustomerRequestDTO walkInCustomer;
    // boatId, tripDate and timeSlot are taken from the selected trip; when sent they must match it.
    private Integer boatId;
    @NotNull
    private Integer tripId;
    private Integer packageId;
    @FutureOrPresent
    private LocalDate tripDate;
    private String timeSlot;
    @NotNull
    @Min(1)
    private Integer numberOfPeople;
    private String paymentStatus;
    private String bookingStatus;

    public BookingRequestDTO() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public WalkInCustomerRequestDTO getWalkInCustomer() {
        return walkInCustomer;
    }

    public void setWalkInCustomer(WalkInCustomerRequestDTO walkInCustomer) {
        this.walkInCustomer = walkInCustomer;
    }

    public Integer getBoatId() {
        return boatId;
    }

    public void setBoatId(Integer boatId) {
        this.boatId = boatId;
    }

    public LocalDate getTripDate() {
        return tripDate;
    }

    public void setTripDate(LocalDate tripDate) {
        this.tripDate = tripDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public Integer getNumberOfPeople() {
        return numberOfPeople;
    }

    public void setNumberOfPeople(Integer numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
    }

    public Integer getTripId() {
        return tripId;
    }

    public void setTripId(Integer tripId) {
        this.tripId = tripId;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }
}
