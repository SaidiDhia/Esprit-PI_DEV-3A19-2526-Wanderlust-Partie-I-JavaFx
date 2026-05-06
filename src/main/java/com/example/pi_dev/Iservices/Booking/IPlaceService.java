package com.example.pi_dev.Iservices.Booking;

import com.example.pi_dev.Entities.Booking.Place;
import java.util.List;

public interface IPlaceService {
    int ajouterPlace(Place p);

    void modifierPlace(Place p);

    void supprimerPlace(int id);

    List<Place> afficherPlaces();

    Place getPlaceById(int id);

    List<Place> afficherParStatus(Place.Status status);

    // Extra methods for UI
    List<Place> findApproved();

    List<Place> findByHost(String hostId);

    List<Place> findPending();

    void updateStatus(int placeId, Place.Status status);
}
