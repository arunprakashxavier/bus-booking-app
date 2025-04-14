package com.guvi.busapp.service;

import com.guvi.busapp.model.Booking;

public interface EmailService {

    /**
     * Sends a booking confirmation email to the user.
     *
     * @param booking The confirmed booking details.
     */
    void sendBookingConfirmation(Booking booking);

}