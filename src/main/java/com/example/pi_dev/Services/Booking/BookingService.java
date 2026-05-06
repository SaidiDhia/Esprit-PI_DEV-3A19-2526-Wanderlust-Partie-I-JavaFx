package com.example.pi_dev.Services.Booking;

import com.example.pi_dev.Entities.Booking.Booking;
import com.example.pi_dev.Iservices.Booking.IBookingService;
import com.example.pi_dev.Utils.Booking.Mydatabase;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class BookingService implements IBookingService {

    private final Connection con;

    public BookingService() {
        con = Mydatabase.getInstance().getConnextion();
    }

    // DTO for booking lists that need place title
    public static class BookingView {
        public final Booking booking;
        public final String placeTitle;

        public BookingView(Booking booking, String placeTitle) {
            this.booking = booking;
            this.placeTitle = placeTitle;
        }
    }

    @Override
    public boolean isAvailable(int placeId, LocalDate start, LocalDate end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalArgumentException("Dates invalides: start doit être avant end");
        }

        String sql = "SELECT COUNT(*) " +
                "FROM booking " +
                "WHERE place_id = ? " +
                "  AND status IN ('PENDING','CONFIRMED') " +
                "  AND NOT (end_date <= ? OR start_date >= ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            ps.setDate(2, java.sql.Date.valueOf(start));
            ps.setDate(3, java.sql.Date.valueOf(end));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur check disponibilité", e);
        }
    }

    @Override
    public void ajouterBooking(Booking b) {
        boolean ok = isAvailable(b.getPlaceId(), b.getStartDate(), b.getEndDate());
        if (!ok) {
            throw new RuntimeException("Place déjà réservée pour ces dates (chevauchement).");
        }

        // Fetch the host_id from the place table
        String hostId = null;
        String placeSql = "SELECT host_id FROM place WHERE id = ?";
        try (PreparedStatement psPlace = con.prepareStatement(placeSql)) {
            psPlace.setInt(1, b.getPlaceId());
            try (ResultSet rs = psPlace.executeQuery()) {
                if (rs.next()) {
                    hostId = rs.getString("host_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du host_id", e);
        }

        String sql = "INSERT INTO booking (place_id, user_id, host_id, start_date, end_date, total_price, guests_count, status) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, b.getPlaceId());
            ps.setString(2, b.getUserId());
            ps.setString(3, hostId);
            ps.setDate(4, java.sql.Date.valueOf(b.getStartDate()));
            ps.setDate(5, java.sql.Date.valueOf(b.getEndDate()));
            ps.setDouble(6, b.getTotalPrice());
            ps.setInt(7, b.getGuestsCount());
            ps.setString(8, Booking.Status.PENDING.name());

            ps.executeUpdate();
            System.out.println("Booking ajouté");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout booking", e);
        }
    }

    @Override
    public void modifierBooking(Booking b) {
        String checkSql = "SELECT COUNT(*) FROM booking " +
                "WHERE place_id=? AND id != ? " +
                "  AND status IN ('PENDING','CONFIRMED') " +
                "  AND NOT (end_date <= ? OR start_date >= ?)";

        try (PreparedStatement check = con.prepareStatement(checkSql)) {
            check.setInt(1, b.getPlaceId());
            check.setInt(2, b.getId());
            check.setDate(3, Date.valueOf(b.getStartDate()));
            check.setDate(4, Date.valueOf(b.getEndDate()));
            try (ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    throw new RuntimeException("Ces dates ne sont pas disponibles.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur vérification disponibilité", e);
        }

        String sql = "UPDATE booking SET place_id=?, user_id=?, host_id=?, start_date=?, end_date=?, total_price=?, guests_count=?, status=? "
                +
                "WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, b.getPlaceId());
            ps.setString(2, b.getUserId());
            ps.setString(3, b.getHostId());
            ps.setDate(4, Date.valueOf(b.getStartDate()));
            ps.setDate(5, Date.valueOf(b.getEndDate()));
            ps.setDouble(6, b.getTotalPrice());
            ps.setInt(7, b.getGuestsCount());
            ps.setString(8, b.getStatus().name());
            ps.setInt(9, b.getId());
            ps.executeUpdate();
            System.out.println("Booking modifié id=" + b.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification booking", e);
        }
    }

    @Override
    public void supprimerBooking(int id) {
        String sql = "DELETE FROM booking WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Booking supprimé id=" + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression booking", e);
        }
    }

    @Override
    public List<Booking> afficherBookings() {
        String sql = "SELECT * FROM booking ORDER BY id DESC";
        List<Booking> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage bookings", e);
        }
        return list;
    }

    @Override
    public Booking getBookingById(int id) {
        String sql = "SELECT * FROM booking WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapBooking(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getBookingById", e);
        }
    }

    /** Alias for getBookingById, used by cancellation logic. */
    public Booking getById(int bookingId) {
        return getBookingById(bookingId);
    }

    @Override
    public List<Booking> afficherBookingsParPlace(int placeId) {
        String sql = "SELECT * FROM booking WHERE place_id=? ORDER BY id DESC";
        List<Booking> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur afficherBookingsParPlace", e);
        }
        return list;
    }

    @Override
    public List<Booking> findByUser(String userId) {
        String sql = "SELECT * FROM booking WHERE user_id=? ORDER BY id DESC";
        List<Booking> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapBooking(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByUser", e);
        }
        return list;
    }

    @Override
    public List<Booking> findPending() {
        String sql = "SELECT * FROM booking WHERE status='PENDING' ORDER BY id DESC";
        List<Booking> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBooking(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findPending bookings", e);
        }
        return list;
    }

    @Override
    public void updateStatus(int bookingId, Booking.Status status) {
        String sql = "UPDATE booking SET status=? WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            ps.executeUpdate();
            System.out.println("Booking status updated: id=" + bookingId + " -> " + status);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur updateStatus booking", e);
        }
    }

    /**
     * Updates the PDF file path for a specific booking.
     */
    public void updatePdfPath(int bookingId, String pdfPath) {
        String sql = "UPDATE booking SET pdf_path=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pdfPath);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
            System.out.println("PDF path updated for bookingId=" + bookingId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur updatePdfPath", e);
        }
    }

    /**
     * Checks if a user is eligible to review a place.
     */
    public boolean canReview(String userId, int placeId) {
        String sql = "SELECT COUNT(*) FROM booking " +
                "WHERE user_id=? AND place_id=? " +
                "AND (status='COMPLETED' OR (status='CONFIRMED' AND end_date < CURDATE()))";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setInt(2, placeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur canReview", e);
        }
    }

    /** Fetch bookings with place title (JOIN) for a specific user. */
    public List<BookingView> findByUserWithTitle(String userId) {
        String sql = "SELECT b.*, p.title AS place_title " +
                "FROM booking b " +
                "JOIN place p ON b.place_id = p.id " +
                "WHERE b.user_id=? ORDER BY b.id DESC";
        List<BookingView> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BookingView(mapBooking(rs), rs.getString("place_title")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByUserWithTitle", e);
        }
        return list;
    }

    /** Fetch PENDING bookings with place title (JOIN) for admin. */
    public List<BookingView> findPendingWithTitle() {
        String sql = "SELECT b.*, p.title AS place_title " +
                "FROM booking b " +
                "JOIN place p ON b.place_id = p.id " +
                "WHERE b.status='PENDING' ORDER BY b.id DESC";
        List<BookingView> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new BookingView(mapBooking(rs), rs.getString("place_title")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findPendingWithTitle", e);
        }
        return list;
    }

    /** Fetch ALL bookings with place title (JOIN) for admin view. */
    public List<BookingView> findAllWithTitle() {
        String sql = "SELECT b.*, p.title AS place_title " +
                "FROM booking b " +
                "JOIN place p ON b.place_id = p.id " +
                "ORDER BY b.id DESC";
        List<BookingView> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new BookingView(mapBooking(rs), rs.getString("place_title")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findAllWithTitle", e);
        }
        return list;
    }

    /** Fetch bookings for a host's places with place title (JOIN). */
    public List<BookingView> findByHostWithTitle(String hostId) {
        String sql = "SELECT b.*, p.title AS place_title " +
                "FROM booking b " +
                "JOIN place p ON b.place_id = p.id " +
                "WHERE b.host_id = ? " +
                "ORDER BY b.id DESC";
        List<BookingView> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, hostId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BookingView(mapBooking(rs), rs.getString("place_title")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur findByHostWithTitle", e);
        }
        return list;
    }

    // =====================================================================
    // CANCELLATION LOGIC
    // =====================================================================

    /**
     * Cancellation by the user.
     * - PENDING → CANCELLED, refund = 0
     * - CONFIRMED → CANCELLED, refund calculated by date policy
     * - REJECTED / COMPLETED → throws RuntimeException (shown in UI)
     *
     * @return the refund amount (0 if none)
     */
    public double cancelByUser(int bookingId) {
        Booking booking = getById(bookingId);
        if (booking == null) {
            throw new RuntimeException("Réservation introuvable (id=" + bookingId + ").");
        }

        Booking.Status status = booking.getStatus();

        if (status == Booking.Status.REJECTED) {
            throw new RuntimeException("Annulation impossible : cette réservation a déjà été rejetée.");
        }
        if (status == Booking.Status.COMPLETED) {
            throw new RuntimeException("Annulation impossible : le séjour est déjà terminé.");
        }
        if (status == Booking.Status.CANCELLED) {
            throw new RuntimeException("Cette réservation est déjà annulée.");
        }

        double refund = 0.0;
        if (status == Booking.Status.CONFIRMED) {
            refund = calculateRefundAmount(booking.getStartDate(), booking.getTotalPrice());
        }
        // PENDING → refund = 0 (no payment was made)

        applyCancellation(bookingId, "USER", refund, null);
        return refund;
    }

    /**
     * Cancellation by the admin (with optional reason).
     * - PENDING or CONFIRMED → CANCELLED, same refund policy
     * - COMPLETED → throws RuntimeException
     *
     * @return the refund amount
     */
    public double cancelByAdmin(int bookingId, String reason) {
        Booking booking = getById(bookingId);
        if (booking == null) {
            throw new RuntimeException("Réservation introuvable (id=" + bookingId + ").");
        }

        Booking.Status status = booking.getStatus();

        if (status == Booking.Status.COMPLETED) {
            throw new RuntimeException("Annulation impossible : le séjour est déjà terminé (COMPLETED).");
        }
        if (status == Booking.Status.CANCELLED) {
            throw new RuntimeException("Cette réservation est déjà annulée.");
        }
        if (status == Booking.Status.REJECTED) {
            throw new RuntimeException("Annulation impossible : réservation déjà rejetée.");
        }

        double refund = 0.0;
        if (status == Booking.Status.CONFIRMED) {
            refund = calculateRefundAmount(booking.getStartDate(), booking.getTotalPrice());
        }
        // PENDING → refund = 0

        applyCancellation(bookingId, "ADMIN", refund, reason);
        return refund;
    }

    /**
     * Calculates the refund based on days before start_date.
     * ≥ 7 days → 100% | 3–6 days → 50% | < 3 days → 0%
     */
    private double calculateRefundAmount(LocalDate startDate, double totalPrice) {
        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), startDate);

        if (daysBefore >= 7) {
            return totalPrice; // 100%
        } else if (daysBefore >= 3) {
            return totalPrice * 0.5; // 50%
        } else {
            return 0.0; // 0%
        }
    }

    /**
     * Internal helper: persists cancellation data to DB.
     */
    private void applyCancellation(int bookingId, String cancelledBy, double refundAmount, String reason) {
        String sql = "UPDATE booking SET status='CANCELLED', cancelled_at=?, refund_amount=?, cancelled_by=?, cancel_reason=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setDouble(2, refundAmount);
            ps.setString(3, cancelledBy);
            ps.setString(4, reason);
            ps.setInt(5, bookingId);
            ps.executeUpdate();
            System.out.println("Booking annulé id=" + bookingId + " by=" + cancelledBy + " refund=" + refundAmount);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur annulation booking", e);
        }
    }

    // =====================================================================

    private Booking mapBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setPlaceId(rs.getInt("place_id"));
        b.setUserId(rs.getString("user_id"));

        // Host ID column (added recently by user)
        try {
            b.setHostId(rs.getString("host_id"));
        } catch (SQLException ignored) { /* column not yet in DB */ }

        b.setStartDate(rs.getDate("start_date").toLocalDate());
        b.setEndDate(rs.getDate("end_date").toLocalDate());
        b.setTotalPrice(rs.getDouble("total_price"));
        b.setGuestsCount(rs.getInt("guests_count"));
        String bookingStatus = rs.getString("status");
        if (bookingStatus == null || bookingStatus.trim().isEmpty()) {
            b.setStatus(Booking.Status.PENDING);
        } else {
            try {
                b.setStatus(Booking.Status.valueOf(bookingStatus.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                b.setStatus(Booking.Status.PENDING);
            }
        }

        // pdf_path column (may be null)
        try {
            b.setPdfPath(rs.getString("pdf_path"));
        } catch (SQLException ignored) {
            /* column not yet in DB */ }

        // Cancellation columns (gracefully handle if ALTER not yet run)
        try {
            Timestamp cancelledAt = rs.getTimestamp("cancelled_at");
            if (cancelledAt != null) {
                b.setCancelledAt(cancelledAt.toLocalDateTime());
            }
            b.setRefundAmount(rs.getDouble("refund_amount"));
            b.setCancelledBy(rs.getString("cancelled_by"));
            b.setCancelReason(rs.getString("cancel_reason"));
        } catch (SQLException ignored) {
            /* columns not yet in DB */ }

        return b;
    }
}
