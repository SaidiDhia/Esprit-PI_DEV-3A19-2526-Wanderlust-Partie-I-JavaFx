module com.example.pi_dev {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;

    // Java standard modules
    requires java.sql;
    requires java.desktop;
    requires java.net.http;
    requires java.mail;
    requires java.prefs;
    requires java.management;
    requires java.logging;
    requires java.naming;
    requires java.xml;

    // JavaFX UI libraries
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    // QR Code libraries
    requires com.google.zxing;
    requires com.google.zxing.javase;

    // JSON processing
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;

    // JWT libraries – the JAR names are jjwt-api, jjwt-impl, jjwt-jackson
    requires jjwt.api;
    requires jjwt.impl;
    requires jjwt.jackson;

    // Webcam capture – the JAR is webcam-capture
    requires webcam.capture;

    // Audio/Media libraries
    requires jaudiotagger; // JAR: jaudiotagger-2.0.3.jar
    requires mp3spi;
    // Cloudinary (if used)
    requires cloudinary.http44;
    requires cloudinary.core;

    // iText PDF (if used)
    requires kernel;
    requires layout;

    // Test dependencies
    requires static org.junit.jupiter.api;
    requires static org.testng;
    requires googleauth;
    requires jdk.jsobject;
    requires itextpdf;
    requires io;

    // Open the entire module for FXGL and other reflective libraries
    opens com.example.pi_dev to javafx.fxml, com.almasb.fxgl.all;

    // Common module
    opens com.example.pi_dev.common.services to javafx.base;
    opens com.example.pi_dev.common.models to javafx.base;

    // Exports
    exports com.example.pi_dev;

    // ADDED: Host package opens and exports
    opens com.example.pi_dev.Controllers.Booking.Host to javafx.fxml;

    exports com.example.pi_dev.Controllers.Booking.Host;

    // Booking controllers - open ALL subpackages
    opens com.example.pi_dev.Controllers.Booking.Front to javafx.fxml;
    opens com.example.pi_dev.Controllers.Booking.Admin to javafx.fxml; // if exists

    // Duplicate opens entry removed: com.example.pi_dev.events.Controllers

    // Export them as needed
    exports com.example.pi_dev.Controllers.Booking.Front;
    exports com.example.pi_dev.common.services;
    exports com.example.pi_dev.common.models;

    exports com.example.pi_dev.Controllers.Blog;

    opens com.example.pi_dev.Controllers.Blog to javafx.fxml;

    exports com.example.pi_dev.Controllers.Booking;

    opens com.example.pi_dev.Controllers.Booking to javafx.fxml;

    exports com.example.pi_dev.Controllers.Events;

    opens com.example.pi_dev.Controllers.Events to javafx.fxml;

    exports com.example.pi_dev.Controllers.Main;

    opens com.example.pi_dev.Controllers.Main to javafx.fxml;

    exports com.example.pi_dev.Controllers.Marketplace;

    opens com.example.pi_dev.Controllers.Marketplace to javafx.fxml;

    exports com.example.pi_dev.Controllers.Messaging;

    opens com.example.pi_dev.Controllers.Messaging to javafx.fxml;

    exports com.example.pi_dev.Controllers.Users;

    opens com.example.pi_dev.Controllers.Users to javafx.fxml;

    exports com.example.pi_dev.Entities.Users;

    opens com.example.pi_dev.Entities.Users to javafx.base;

    exports com.example.pi_dev.Entities.Messaging;

    opens com.example.pi_dev.Entities.Messaging to javafx.base;

    exports com.example.pi_dev.Entities.Events;
    exports com.example.pi_dev.Entities.Blog;
    exports com.example.pi_dev.Entities.Booking;
    exports com.example.pi_dev.enums;

    opens com.example.pi_dev.Entities.Blog to javafx.base;
    opens com.example.pi_dev.Entities.Booking to javafx.base;

    exports com.example.pi_dev.Iservices.Events;
    exports com.example.pi_dev.Services.Users;

    opens com.example.pi_dev.Services.Users to javafx.base;

    exports com.example.pi_dev.Services.Messaging;

    opens com.example.pi_dev.Services.Messaging to javafx.base;

    exports com.example.pi_dev.Services.Events;
    exports com.example.pi_dev.Services.Blog;
    exports com.example.pi_dev.Services.Booking;
    exports com.example.pi_dev.Test.Booking to javafx.graphics;

    opens com.example.pi_dev.Test.Booking to javafx.fxml;

    exports com.example.pi_dev.Test.Marketplace to javafx.graphics;

    opens com.example.pi_dev.Test.Marketplace to javafx.fxml;

    exports com.example.pi_dev.Utils.Events;
    exports com.example.pi_dev.Utils.Users;

    opens com.example.pi_dev.Utils.Users to javafx.base;

    exports com.example.pi_dev.BlogGUI;
    exports com.example.pi_dev.Database.Users;

    opens com.example.pi_dev.Database.Users to javafx.base;

    exports com.example.pi_dev.Repositories.Messaging;

    opens com.example.pi_dev.Repositories.Messaging to javafx.base;

    exports com.example.pi_dev.Repositories.Users;

    opens com.example.pi_dev.Repositories.Users to javafx.base;

    exports com.example.pi_dev.Session;

    opens com.example.pi_dev.Session to javafx.base;
}
