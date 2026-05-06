package com.example.pi_dev.Iservices.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import java.sql.SQLException;
import java.util.List;

public interface IReservationService {

    // CREATE
    void ajouter(Reservation reservation) throws SQLException;

    // READ
    List<Reservation> afficher() throws SQLException;
    Reservation getById(int id) throws SQLException;
    List<Reservation> getByEvent(int idEvent) throws SQLException;

    // UPDATE
    void modifier(Reservation reservation) throws SQLException;
    void changerStatut(int idReservation, Reservation.StatutReservation statut) throws SQLException;

    // DELETE
    void supprimer(int id) throws SQLException;
}