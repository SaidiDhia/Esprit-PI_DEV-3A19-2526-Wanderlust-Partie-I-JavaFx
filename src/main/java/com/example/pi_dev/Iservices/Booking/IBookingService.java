package com.example.pi_dev.Iservices.Booking;

import com.example.pi_dev.Entities.Booking.Booking;
import java.time.LocalDate;
import java.util.List;

public interface IBookingService {
    void ajouterBooking(Booking b);

    void modifierBooking(Booking b);

    void supprimerBooking(int id);

    List<Booking> afficherBookings();

    Booking getBookingById(int id);

    List<Booking> afficherBookingsParPlace(int placeId);

    // Extra methods for UI
    boolean isAvailable(int placeId, LocalDate start, LocalDate end);

    List<Booking> findByUser(String userId);

    List<Booking> findPending();

    void updateStatus(int bookingId, Booking.Status status);
}
