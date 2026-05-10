package com.example.pi_dev.Services.Events;

import com.example.pi_dev.Entities.Events.Event;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DynamicPricingEngine {

    public static class PricingResult {
        public double originalPrice;
        public double finalPrice;
        public double discountPercentage;
        public double savings;
        public String discountReason;

        public PricingResult(double originalPrice, double finalPrice, double discountPercentage, double savings, String discountReason) {
            this.originalPrice = originalPrice;
            this.finalPrice = finalPrice;
            this.discountPercentage = discountPercentage;
            this.savings = savings;
            this.discountReason = discountReason;
        }
    }

    public static PricingResult calculatePrice(Event event) {
        if (event == null || event.getPrix() == null || event.getCapaciteMax() <= 0) {
            double base = event != null && event.getPrix() != null ? event.getPrix().doubleValue() : 0.0;
            return new PricingResult(base, base, 0.0, 0.0, "Aucune");
        }

        double basePrice = event.getPrix().doubleValue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime eventStart = event.getDateDebut();

        // 1. Facteur temps (Time Factor)
        double timeFactor;
        if (eventStart == null || eventStart.isBefore(now)) {
            timeFactor = 1.0;
        } else {
            long hoursUntil = ChronoUnit.HOURS.between(now, eventStart);
            if (hoursUntil > 168) timeFactor = 0.1;
            else if (hoursUntil > 72) timeFactor = 0.3;
            else if (hoursUntil > 24) timeFactor = 0.6;
            else if (hoursUntil > 12) timeFactor = 0.8;
            else timeFactor = 1.0;
        }

        // 2. Facteur remplissage (Occupancy Factor)
        double capacity = event.getCapaciteMax();
        double available = event.getPlacesDisponibles();
        double occupied = capacity - available;
        double occupancyRate = occupied / capacity;
        double threshold = 0.7;

        double occupancyFactor;
        if (occupancyRate >= threshold) occupancyFactor = 0.1;
        else if (occupancyRate >= threshold * 0.8) occupancyFactor = 0.4;
        else if (occupancyRate >= threshold * 0.6) occupancyFactor = 0.7;
        else occupancyFactor = 1.0;

        // 3. Facteur popularité (Simulation)
        double popularityFactor = 0.6;

        // 4. Calcul du score composite
        double timeWeight = 0.40;
        double occupancyWeight = 0.35;
        double popularityWeight = 0.25;

        double urgencyScore = (timeFactor * timeWeight) + 
                              (occupancyFactor * occupancyWeight) + 
                              (popularityFactor * popularityWeight);

        // 5. Calcul de la réduction
        double discountPercentage = Math.pow(urgencyScore, 1.5) * 0.4;
        
        // 6. Prix plancher émotionnel (50%)
        double emotionalFloor = basePrice * 0.50;

        // 7. Prix final
        double newPrice = Math.max(basePrice * (1.0 - discountPercentage), emotionalFloor);
        double savings = basePrice - newPrice;

        // 8. Raison du changement
        String reason;
        double maxFactor = Math.max(timeFactor, Math.max(occupancyFactor, popularityFactor));
        if (maxFactor == timeFactor) reason = "Urgence temporelle";
        else if (maxFactor == occupancyFactor) reason = "Places limitées (Faible remplissage)";
        else reason = "Boost de popularité";
        
        // Si la réduction est très faible (ex: < 1%), on l'ignore
        if (discountPercentage < 0.01) {
            return new PricingResult(basePrice, basePrice, 0.0, 0.0, "Aucune");
        }

        return new PricingResult(basePrice, newPrice, discountPercentage, savings, reason);
    }
}
