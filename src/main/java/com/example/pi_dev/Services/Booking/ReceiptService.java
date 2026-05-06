package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Entities.Booking.Place;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Generates an HTML booking receipt that can be opened in a browser
 * and saved/printed as PDF by the user.
 */
public class ReceiptService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Generates an HTML receipt file for the given booking + place.
     *
     * @return the Path to the saved HTML file
     */
    public Path generateReceipt(Booking booking, Place place) throws IOException {
        Path dir = Paths.get("uploads", "receipts");
        Files.createDirectories(dir);

        String fileName = "receipt_booking_" + booking.getId() + ".html";
        Path file = dir.resolve(fileName);

        String html = buildHtml(booking, place);
        Files.writeString(file, html, StandardCharsets.UTF_8);
        return file;
    }

    private String buildHtml(Booking booking, Place place) {
        long nights = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
        String startFmt = booking.getStartDate().format(FMT);
        String endFmt = booking.getEndDate().format(FMT);
        String today = LocalDate.now().format(FMT);
        String placeName = place != null ? place.getTitle() : "N/A";
        String placeAddr = place != null ? (place.getAddress() + ", " + place.getCity()) : "N/A";
        double pricePerDay = place != null ? place.getPricePerDay() : 0;

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/>" +
                "<title>Booking Receipt #" + booking.getId() + "</title>" +
                "<style>" +
                "body{font-family:'Segoe UI',Arial,sans-serif;background:#F3F4F6;margin:0;padding:40px;}" +
                ".receipt{max-width:680px;margin:0 auto;background:#fff;border-radius:16px;" +
                "box-shadow:0 4px 24px rgba(0,0,0,0.10);padding:48px;}" +
                ".header{display:flex;justify-content:space-between;align-items:center;margin-bottom:32px;}" +
                ".logo{font-size:28px;font-weight:800;color:#2563EB;letter-spacing:-1px;}" +
                ".receipt-no{font-size:13px;color:#6B7280;text-align:right;}" +
                "h2{color:#111827;font-size:22px;margin:0 0 20px;}" +
                ".badge{display:inline-block;padding:4px 14px;border-radius:20px;font-size:12px;font-weight:700;" +
                "background:#DBEAFE;color:#1D4ED8;}" +
                "table{width:100%;border-collapse:collapse;margin:24px 0;}" +
                "th{text-align:left;color:#6B7280;font-size:12px;text-transform:uppercase;padding:8px 0;border-bottom:2px solid #E5E7EB;}"
                +
                "td{padding:10px 0;border-bottom:1px solid #F3F4F6;color:#374151;}" +
                "td.label{font-weight:600;color:#111827;width:45%;}" +
                ".total-row td{border-top:2px solid #E5E7EB;font-size:18px;font-weight:800;color:#2563EB;padding-top:14px;}"
                +
                ".footer{margin-top:36px;text-align:center;color:#9CA3AF;font-size:12px;}" +
                "@media print{body{background:#fff;padding:0;}.receipt{box-shadow:none;}}" +
                "</style></head><body>" +
                "<div class='receipt'>" +
                "<div class='header'>" +
                "<div class='logo'>🏠 Wanderlust</div>" +
                "<div class='receipt-no'>Booking Receipt<br/>Reference: <strong>#" + booking.getId() + "</strong><br/>"
                +
                "Date: " + today + "</div>" +
                "</div>" +
                "<h2>Booking Confirmation</h2>" +
                "<span class='badge'>" + booking.getStatus().name() + "</span>" +
                "<table><thead><tr><th>Details</th><th></th></tr></thead><tbody>" +
                row("Property", placeName) +
                row("Address", placeAddr) +
                row("Check-in", startFmt) +
                row("Check-out", endFmt) +
                row("Duration", nights + " night" + (nights > 1 ? "s" : "")) +
                row("Guests", String.valueOf(booking.getGuestsCount())) +
                row("Price per night", String.format("$%.2f", pricePerDay)) +
                "</tbody></table>" +
                "<table><tbody><tr class='total-row'>" +
                "<td class='label'>Total Amount</td>" +
                "<td style='text-align:right;'>" + String.format("$%.2f", booking.getTotalPrice()) + "</td>" +
                "</tr></tbody></table>" +
                "<div class='footer'>Thank you for choosing Wanderlust!<br/>" +
                "This is an automatically generated receipt. Please keep it for your records.</div>" +
                "</div></body></html>";
    }

    private String row(String label, String value) {
        return "<tr><td class='label'>" + label + "</td><td>" + value + "</td></tr>";
    }
}
