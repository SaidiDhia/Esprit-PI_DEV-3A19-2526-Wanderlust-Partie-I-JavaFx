package com.example.pi_dev.Iservices.Events;

import com.example.pi_dev.Entities.Events.Event;
import java.sql.SQLException;
import java.util.List;

public interface IEventService {

    // CREATE
    void ajouter(Event event) throws SQLException;

    // READ
    List<Event> afficher() throws SQLException;
    Event getById(int id) throws SQLException;
    List<Event> getByActivite(int idActivite) throws SQLException;

    // UPDATE
    void modifier(Event event) throws SQLException;
    void diminuerPlaces(int idEvent, int nombre) throws SQLException;
    void augmenterPlaces(int idEvent, int nombre) throws SQLException;

    // DELETE
    void supprimer(int id) throws SQLException;
}