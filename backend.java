import javafx.collections.ObservableList;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.sql.Date;
import java.sql.Time;


class AirlineBackendService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/airline_db";
    private static final String USER = "root";
    private static final String PASS = "*Kihtrak7141#";
    private Connection conn;

    public void initializeDatabaseConnection() throws SQLException {
        conn = DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public List<AirlineBookingSystemFrontend.Flight> searchFlights(String origin, String destination, Date journeyDate) throws SQLException {
        List<AirlineBookingSystemFrontend.Flight> flights = new ArrayList<>();

        LocalDate journeyLocalDate = journeyDate.toLocalDate();
        LocalDate today = LocalDate.now();
        boolean isToday = journeyLocalDate.equals(today);

        String query = "SELECT f.Flight_id, f.Airline, a1.Airport_name as OriginName, " +
                "a2.Airport_name as DestName, f.Departure_time, f.Arrival_time, " +
                "f.Price, fa.Available_Seats " +
                "FROM FLIGHTS f " +
                "JOIN AIRPORT a1 ON f.Origin = a1.Airport_id " +
                "JOIN AIRPORT a2 ON f.Destination = a2.Airport_id " +
                "JOIN FLIGHT_AVAILABILITY fa ON f.Flight_id = fa.Flight_id " +
                "WHERE f.Origin = ? AND f.Destination = ? " +
                "AND fa.Journey_Date = ? " +
                "AND fa.Available_Seats > 0 ";

        if (isToday) {
            query += "AND f.Departure_time > ? ";
        }

        query += "ORDER BY f.Departure_time";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, destination);
            pstmt.setDate(3, journeyDate);

            if (isToday) {
                pstmt.setTime(4, Time.valueOf(LocalTime.now()));
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                AirlineBookingSystemFrontend.Flight flight = new AirlineBookingSystemFrontend.Flight(
                        rs.getInt("Flight_id"),
                        rs.getString("Airline"),
                        rs.getString("OriginName"),
                        rs.getString("DestName"),
                        rs.getTime("Departure_time"),
                        rs.getTime("Arrival_time"),
                        rs.getDouble("Price")
                );
                flights.add(flight);
            }
        }
        return flights;
    }
    public String bookTicket(int flightId, Date journeyDate, ObservableList<AirlineBookingSystemFrontend.Passenger> passengers, String paymentMethod) throws SQLException {
        String pnr = generateUniquePNR();
        List<Integer> passengerIds = new ArrayList<>();

        try {
            conn.setAutoCommit(false);

            for (AirlineBookingSystemFrontend.Passenger passenger : passengers) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO PASSENGERS (Name, Email_id, Phone_num) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, passenger.getName());
                    pstmt.setString(2, passenger.getEmail());
                    pstmt.setString(3, passenger.getPhone());
                    pstmt.executeUpdate();

                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            passengerIds.add(generatedKeys.getInt(1));
                        }
                    }
                }
            }

            Set<String> assignedSeats = new HashSet<>();

            for (int passengerId : passengerIds) {
                String seatNo;
                do {
                    seatNo = generateSeatNumber();
                } while (assignedSeats.contains(seatNo));
                assignedSeats.add(seatNo);

                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO RESERVATION (PNR, Passenger_ID, Flight_ID, Seat_No, Booking_Date, Journey_Date) " +
                                "VALUES (?, ?, ?, ?, CURDATE(), ?)")) {
                    pstmt.setString(1, pnr);
                    pstmt.setInt(2, passengerId);
                    pstmt.setInt(3, flightId);
                    pstmt.setString(4, seatNo);
                    pstmt.setDate(5, (java.sql.Date) journeyDate);
                    pstmt.executeUpdate();
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE FLIGHT_AVAILABILITY SET Available_Seats = Available_Seats - ? " +
                            "WHERE Flight_id = ? AND Journey_Date = ?")) {
                pstmt.setInt(1, passengers.size());
                pstmt.setInt(2, flightId);
                pstmt.setDate(3, (java.sql.Date) journeyDate);
                pstmt.executeUpdate();
            }

            double totalAmount = getFlightPrice(flightId) * passengers.size();

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO PAYMENT (PNR, Amount, Payment_Method, Transaction_date) VALUES (?, ?, ?, NOW())")) {
                pstmt.setString(1, pnr);
                pstmt.setDouble(2, totalAmount);
                pstmt.setString(3, paymentMethod);
                pstmt.executeUpdate();
            }

            conn.commit();
            return pnr;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }
    public List<AirlineBookingSystemFrontend.Reservation> getAllReservations() throws SQLException {
        List<AirlineBookingSystemFrontend.Reservation> reservations = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM RESERVATION")) {

            while (rs.next()) {
                reservations.add(new AirlineBookingSystemFrontend.Reservation(
                        rs.getString("PNR"),
                        rs.getInt("Passenger_ID"),
                        rs.getInt("Flight_ID"),
                        rs.getString("Seat_No"),
                        rs.getDate("Booking_Date"),
                        rs.getDate("Journey_Date")
                ));
            }
        }
        return reservations;
    }

    public void addBaggage(int passengerId, String pnr, double weight, String type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO BAGGAGE (Passenger_ID, PNR, Weight, Baggage_type) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, passengerId);
            ps.setString(2, pnr);
            ps.setDouble(3, weight);
            ps.setString(4, type);
            ps.executeUpdate();
        }
    }

    public List<AirlineBookingSystemFrontend.Baggage> getBaggageForPNR(String pnr) throws SQLException {
        List<AirlineBookingSystemFrontend.Baggage> baggageList = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM BAGGAGE WHERE PNR = ?")) {
            ps.setString(1, pnr);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                baggageList.add(new AirlineBookingSystemFrontend.Baggage(
                        rs.getInt("Baggage_ID"),
                        rs.getInt("Passenger_ID"),
                        rs.getString("PNR"),
                        rs.getDouble("Weight"),
                        rs.getString("Baggage_type")
                ));
            }
        }
        return baggageList;
    }

    public List<AirlineBookingSystemFrontend.PassengerComboItem> getPassengersForPNR(String pnr) throws SQLException {
        List<AirlineBookingSystemFrontend.PassengerComboItem> passengers = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.Passenger_ID, p.Name " +
                        "FROM PASSENGERS p " +
                        "JOIN RESERVATION r ON p.Passenger_ID = r.Passenger_ID " +
                        "WHERE r.PNR = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    passengers.add(new AirlineBookingSystemFrontend.PassengerComboItem(
                            rs.getInt("Passenger_ID"),
                            rs.getString("Name")
                    ));
                }
            }
        }
        return passengers;
    }

    public Map<Integer, String> getPassengersForPNRUPDATE(String pnr) throws SQLException {
        Map<Integer, String> passengers = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT p.Passenger_ID, p.Name " +
                        "FROM PASSENGERS p " +
                        "JOIN RESERVATION r ON p.Passenger_ID = r.Passenger_ID " +
                        "WHERE r.PNR = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    passengers.put(
                            rs.getInt("Passenger_ID"),
                            rs.getString("Name")
                    );
                }
            }
        }
        return passengers;
    }

    public double getTotalBaggageWeight(int passengerId, String pnr, String type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(Weight), 0) AS total_weight FROM BAGGAGE " +
                        "WHERE Passenger_ID = ? AND PNR = ? AND Baggage_type = ?")) {
            ps.setInt(1, passengerId);
            ps.setString(2, pnr);
            ps.setString(3, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_weight");
                }
            }
        }
        return 0;
    }

    public List<AirlineBookingSystemFrontend.Flight> getAllFlights() throws SQLException {
        List<AirlineBookingSystemFrontend.Flight> flights = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM FLIGHTS ORDER BY DEPARTURE_TIME")) {

            while (rs.next()) {
                flights.add(new AirlineBookingSystemFrontend.Flight(
                        rs.getInt("Flight_id"),
                        rs.getString("Airline"),
                        rs.getString("Origin"),
                        rs.getString("Destination"),
                        rs.getTime("Departure_time"),
                        rs.getTime("Arrival_time"),
                        rs.getDouble("Price")
                ));
            }
        }
        return flights;
    }

    public List<Integer> getAllFlightIds() throws SQLException {
        List<Integer> flightIds = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Flight_id FROM FLIGHTS ORDER BY Flight_id")) {
            while (rs.next()) {
                flightIds.add(rs.getInt("Flight_id"));
            }
        }
        return flightIds;
    }

    public List<AirlineBookingSystemFrontend.Passenger> getPassengersForFlight(int flightId, Date journeyDate) throws SQLException {
        List<AirlineBookingSystemFrontend.Passenger> passengers = new ArrayList<>();
        String query = "SELECT p.Passenger_id, p.Name, p.Email_id, p.Phone_num " +
                "FROM RESERVATION r " +
                "JOIN PASSENGERS p ON r.Passenger_ID = p.Passenger_id " +
                "WHERE r.Flight_ID = ? AND r.Journey_Date = ? " +
                "ORDER BY p.Name";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, flightId);
            pstmt.setDate(2, (java.sql.Date) journeyDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                passengers.add(new AirlineBookingSystemFrontend.Passenger(
                        rs.getInt("Passenger_id"),
                        rs.getString("Name"),
                        rs.getString("Email_id"),
                        rs.getString("Phone_num")
                ));
            }
        }
        return passengers;
    }

    public List<AirlineBookingSystemFrontend.Payment> getAllPayments() throws SQLException {
        List<AirlineBookingSystemFrontend.Payment> payments = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM PAYMENT")) {

            while (rs.next()) {
                payments.add(new AirlineBookingSystemFrontend.Payment(
                        rs.getInt("Payment_ID"),
                        rs.getString("PNR"),
                        rs.getDouble("Amount"),
                        rs.getString("Payment_Method"),
                        rs.getTimestamp("Transaction_date")
                ));
            }
        }
        return payments;
    }

    public List<AirlineBookingSystemFrontend.Reservation> getReservationsByPNR(String pnr) throws SQLException {
        List<AirlineBookingSystemFrontend.Reservation> reservations = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT * FROM RESERVATION WHERE PNR = ?")) {
            pstmt.setString(1, pnr);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                reservations.add(new AirlineBookingSystemFrontend.Reservation(
                        rs.getString("PNR"),
                        rs.getInt("Passenger_ID"),
                        rs.getInt("Flight_ID"),
                        rs.getString("Seat_No"),
                        rs.getDate("Booking_Date"),
                        rs.getDate("Journey_Date")
                ));
            }
        }
        return reservations;
    }

    public void cancelTicket(String pnr) throws SQLException {
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement checkPs = conn.prepareStatement(
                    "SELECT Flight_ID, Journey_Date, COUNT(*) AS PassengerCount FROM RESERVATION WHERE PNR = ? GROUP BY Flight_ID, Journey_Date")) {
                checkPs.setString(1, pnr);
                ResultSet rs = checkPs.executeQuery();

                if (rs.next()) {
                    int flightId = rs.getInt("Flight_ID");
                    Date journeyDate = rs.getDate("Journey_Date");
                    int passengerCount = rs.getInt("PassengerCount");

                    List<Integer> passengerIds = new ArrayList<>();
                    try (PreparedStatement getPassengersPs = conn.prepareStatement(
                            "SELECT Passenger_ID FROM RESERVATION WHERE PNR = ?")) {
                        getPassengersPs.setString(1, pnr);
                        ResultSet passengerRs = getPassengersPs.executeQuery();

                        while (passengerRs.next()) {
                            passengerIds.add(passengerRs.getInt("Passenger_ID"));
                        }
                    }

                    try (PreparedStatement deleteBaggagePs = conn.prepareStatement(
                            "DELETE FROM BAGGAGE WHERE PNR = ?")) {
                        deleteBaggagePs.setString(1, pnr);
                        deleteBaggagePs.executeUpdate();
                    }

                    try (PreparedStatement deleteReservationsPs = conn.prepareStatement(
                            "DELETE FROM RESERVATION WHERE PNR = ?")) {
                        deleteReservationsPs.setString(1, pnr);
                        deleteReservationsPs.executeUpdate();
                    }

                    if (!passengerIds.isEmpty()) {
                        String placeholders = String.join(",", Collections.nCopies(passengerIds.size(), "?"));
                        String deletePassengersQuery = "DELETE FROM PASSENGERS WHERE Passenger_ID IN (" + placeholders + ")";
                        try (PreparedStatement deletePassengersPs = conn.prepareStatement(deletePassengersQuery)) {
                            for (int i = 0; i < passengerIds.size(); i++) {
                                deletePassengersPs.setInt(i + 1, passengerIds.get(i));
                            }
                            deletePassengersPs.executeUpdate();
                        }
                    }

                    try (PreparedStatement updateSeatsPs = conn.prepareStatement(
                            "UPDATE FLIGHT_AVAILABILITY SET Available_Seats = Available_Seats + ? " +
                                    "WHERE Flight_ID = ? AND Journey_Date = ?")) {
                        updateSeatsPs.setInt(1, passengerCount);
                        updateSeatsPs.setInt(2, flightId);
                        updateSeatsPs.setDate(3, (java.sql.Date) journeyDate);
                        updateSeatsPs.executeUpdate();
                    }
                }
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public void updatePassengerDetails(int passengerId, String detailType, String newValue)
            throws SQLException, IllegalArgumentException {

        String column;
        if ("Email".equalsIgnoreCase(detailType)) {
            column = "Email_id";
        } else if ("Phone".equalsIgnoreCase(detailType)) {
            column = "Phone_num";
        } else {
            throw new IllegalArgumentException("Invalid detail type. Only 'Email' or 'Phone' are allowed.");
        }

        String currentValue = null;
        try (PreparedStatement selectPs = conn.prepareStatement(
                "SELECT " + column + " FROM PASSENGERS WHERE Passenger_ID = ?")) {
            selectPs.setInt(1, passengerId);
            try (ResultSet rs = selectPs.executeQuery()) {
                if (rs.next()) {
                    currentValue = rs.getString(column);
                }
            }
        }

        if (newValue.equals(currentValue)) {
            throw new IllegalStateException("New " + detailType + " is same as current " + detailType);
        }

        try (PreparedStatement updatePs = conn.prepareStatement(
                "UPDATE PASSENGERS SET " + column + " = ? WHERE Passenger_ID = ?")) {
            updatePs.setString(1, newValue);
            updatePs.setInt(2, passengerId);
            int rowsAffected = updatePs.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No passenger found with ID: " + passengerId);
            }
        }
    }

    public List<AirlineBookingSystemFrontend.FlightAvailability> checkFlightAvailability(String origin, String destination, Date date) throws SQLException {
        List<AirlineBookingSystemFrontend.FlightAvailability> flights = new ArrayList<>();
        String query = "SELECT f.Flight_id, f.Airline, a1.Airport_name as Origin, " +
                "a2.Airport_name as Destination, f.Departure_time, fa.Available_Seats " +
                "FROM FLIGHTS f " +
                "JOIN AIRPORT a1 ON f.Origin = a1.Airport_id " +
                "JOIN AIRPORT a2 ON f.Destination = a2.Airport_id " +
                "JOIN FLIGHT_AVAILABILITY fa ON f.Flight_id = fa.Flight_id " +
                "WHERE f.Origin = ? AND f.Destination = ? " +
                "AND fa.Journey_Date = ? " +
                "ORDER BY f.Departure_time";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, origin);
            pstmt.setString(2, destination);
            pstmt.setDate(3, (java.sql.Date) date);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                flights.add(new AirlineBookingSystemFrontend.FlightAvailability(
                        rs.getInt("Flight_id"),
                        rs.getString("Airline"),
                        rs.getString("Origin") + " → " + rs.getString("Destination"),
                        rs.getTime("Departure_time"),
                        rs.getInt("Available_Seats")
                ));
            }
        }
        return flights;
    }

    public List<AirlineBookingSystemFrontend.Baggage> getAllBaggage() throws SQLException {
        List<AirlineBookingSystemFrontend.Baggage> baggageList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM BAGGAGE")) {

            while (rs.next()) {
                baggageList.add(new AirlineBookingSystemFrontend.Baggage(
                        rs.getInt("Baggage_ID"),
                        rs.getInt("Passenger_ID"),
                        rs.getString("PNR"),
                        rs.getDouble("Weight"),
                        rs.getString("Baggage_type")
                ));
            }
        }
        return baggageList;
    }

    public List<String> getAllAirports() throws SQLException {
        List<String> airports = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT Airport_id, Airport_name, City FROM AIRPORT")) {

            while (rs.next()) {
                String airportInfo = rs.getString("Airport_id") + " - " +
                        rs.getString("Airport_name") + " (" +
                        rs.getString("City") + ")";
                airports.add(airportInfo);
            }
        }
        return airports;
    }

    public int getAvailableSeats(int flightId, Date journeyDate) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Available_Seats FROM FLIGHT_AVAILABILITY WHERE Flight_id = ? AND Journey_Date = ?")) {
            pstmt.setInt(1, flightId);
            pstmt.setDate(2, (java.sql.Date) journeyDate);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt("Available_Seats") : 0;
        }
    }

    public String getSeatNumberForPassenger(int passengerId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Seat_No FROM RESERVATION WHERE Passenger_ID = ?")) {
            pstmt.setInt(1, passengerId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("Seat_No") : "";
        }
    }

    public String getPnrForPassenger(int passengerId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT PNR FROM RESERVATION WHERE Passenger_ID = ?")) {
            pstmt.setInt(1, passengerId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("PNR") : "";
        }
    }

    public double getFlightPrice(int flightId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT Price FROM FLIGHTS WHERE Flight_id = ?")) {
            pstmt.setInt(1, flightId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getDouble("Price") : 0;
        }
    }

    private String generateUniquePNR() throws SQLException {
        String pnr;
        int attempts = 0;
        final int MAX_ATTEMPTS = 10;

        do {
            pnr = generateRandomPNR();
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT 1 FROM RESERVATION WHERE PNR = ?")) {
                pstmt.setString(1, pnr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        return pnr;
                    }
                }
            }

            if (++attempts >= MAX_ATTEMPTS) {
                throw new SQLException("Failed to generate unique PNR after " + MAX_ATTEMPTS + " attempts");
            }
        } while (true);
    }

    private String generateRandomPNR() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateSeatNumber() {
        char row = (char) ('A' + new Random().nextInt(10));
        int seat = new Random().nextInt(6) + 1;
        return row + String.valueOf(seat);
    }
}