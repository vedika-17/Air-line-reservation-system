import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import java.sql.Date;
import java.util.List;

public class AirlineBookingSystemFrontend extends Application {

    private final AirlineBackendService backendService = new AirlineBackendService();


    public AirlineBookingSystemFrontend() {
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            backendService.initializeDatabaseConnection();

            VBox mainMenu = createMainMenu(primaryStage);
            Scene scene = new Scene(mainMenu, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            primaryStage.setScene(scene);

            primaryStage.setTitle("Airline Booking System");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage());
        }
    }

    private VBox createMainMenu(Stage primaryStage) {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB, #E0F7FF);");

        Label title = new Label("Airline Booking System");
        title.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #005792;");

        Button bookTicketBtn = createStyledButton("Book Ticket", "✈", "#1E90FF");
        bookTicketBtn.setOnAction(e -> showBookTicketWindow());

        Button viewReservationsBtn = createStyledButton("View Reservations", "📋", "#4682B4");
        viewReservationsBtn.setOnAction(e -> showViewReservationsWindow());

        Button addBaggageBtn = createStyledButton("Add Baggage", "🧳", "#5F9EA0");
        addBaggageBtn.setOnAction(e -> showAddBaggageWindow());

        Button viewFlightsBtn = createStyledButton("View Flights", "🛫", "#00BFFF");
        viewFlightsBtn.setOnAction(e -> showViewFlightsWindow());

        Button viewPassengersBtn = createStyledButton("View Passengers", "👥", "#6495ED");
        viewPassengersBtn.setOnAction(e -> showViewPassengersWindow());

        Button viewPaymentsBtn = createStyledButton("View Payments", "💳", "#4169E1");
        viewPaymentsBtn.setOnAction(e -> showViewPaymentsWindow());

        Button cancelTicketBtn = createStyledButton("Cancel Ticket", "❌", "#DC143C");
        cancelTicketBtn.setOnAction(e -> showCancelTicketWindow());

        Button updatePassengerBtn = createStyledButton("Update Passenger", "✏", "#20B2AA");
        updatePassengerBtn.setOnAction(e -> showUpdatePassengerWindow());

        Button checkAvailabilityBtn = createStyledButton("Check Availability", "🔍", "#008B8B");
        checkAvailabilityBtn.setOnAction(e -> showCheckAvailabilityWindow());

        Button showBaggageBtn = createStyledButton("Show All Baggage", "🧳", "#48D1CC");
        showBaggageBtn.setOnAction(e -> showAllBaggageWindow());

        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(15);
        buttonGrid.setVgap(15);
        buttonGrid.setAlignment(Pos.CENTER);


        buttonGrid.addRow(0, bookTicketBtn, viewReservationsBtn, addBaggageBtn);
        buttonGrid.addRow(1, viewFlightsBtn, viewPassengersBtn, viewPaymentsBtn);
        buttonGrid.addRow(2, cancelTicketBtn, updatePassengerBtn, checkAvailabilityBtn);
        buttonGrid.add(showBaggageBtn, 0, 3, 3, 1);

        vbox.getChildren().addAll(title, buttonGrid);
        return vbox;
    }

    private Button createStyledButton(String text, String icon, String color) {
        Button button = new Button(text);
        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 24;");
        button.setGraphic(iconText);
        button.setContentDisplay(ContentDisplay.TOP);
        button.setPrefSize(180, 100);
        button.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-background-color: " + color + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);");


        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-background-color: derive(" + color + ", 20%); " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 7, 0, 0, 2);"));

        button.setOnMouseExited(e -> button.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-background-color: " + color + "; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 1);"));

        return button;
    }

    private void showBookTicketWindow() {
        Stage stage = new Stage();
        stage.setTitle("Book Ticket");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        DatePicker journeyDatePicker = new DatePicker();
        LocalDate today = LocalDate.now();
        journeyDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(today) || date.isAfter(today.plusMonths(3)));
            }
        });

        ComboBox<String> originComboBox = new ComboBox<>();
        ComboBox<String> destinationComboBox = new ComboBox<>();
        initializeAirportComboBoxes(originComboBox, destinationComboBox);


        TableView<Flight> flightsTable = new TableView<>();
        TableColumn<Flight, Integer> flightIdCol = new TableColumn<>("Flight ID");
        TableColumn<Flight, String> airlineCol = new TableColumn<>("Airline");
        TableColumn<Flight, String> routeCol = new TableColumn<>("Route");
        TableColumn<Flight, Time> departureCol = new TableColumn<>("Departure");
        TableColumn<Flight, Time> arrivalCol = new TableColumn<>("Arrival");
        TableColumn<Flight, Double> priceCol = new TableColumn<>("Price");
        TableColumn<Flight, Integer> seatsCol = new TableColumn<>("Available Seats");

        flightIdCol.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        airlineCol.setCellValueFactory(new PropertyValueFactory<>("airline"));
        routeCol.setCellValueFactory(cellData ->
                Bindings.createStringBinding(() ->
                        cellData.getValue().getOrigin() + " → " + cellData.getValue().getDestination()
                )
        );
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        seatsCol.setCellValueFactory(cellData -> {
            return Bindings.createIntegerBinding(() ->
                    backendService.getAvailableSeats(cellData.getValue().getFlightId(),
                            Date.valueOf(journeyDatePicker.getValue()))).asObject();
        });

        flightsTable.getColumns().addAll(flightIdCol, airlineCol, routeCol, departureCol, arrivalCol, priceCol, seatsCol);


        TextField nameField = new TextField();
        TextField emailField = new TextField();
        TextField phoneField = new TextField();


        TableView<Passenger> passengersTable = new TableView<>();
        TableColumn<Passenger, String> nameCol = new TableColumn<>("Name");
        TableColumn<Passenger, String> emailCol = new TableColumn<>("Email");
        TableColumn<Passenger, String> phoneCol = new TableColumn<>("Phone");

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        passengersTable.getColumns().addAll(nameCol, emailCol, phoneCol);

        ObservableList<Passenger> passengers = FXCollections.observableArrayList();


        Label totalAmountLabel = new Label("₹0.00");
        ComboBox<String> paymentMethodComboBox = new ComboBox<>();
        paymentMethodComboBox.getItems().addAll("Credit Card", "Debit Card", "UPI", "Net Banking");


        Button searchFlightsBtn = new Button("Search Flights");
        Button selectFlightBtn = new Button("Select Flight");
        Button addPassengerBtn = new Button("Add Passenger");
        Button bookTicketBtn = new Button("Book Ticket");

        GridPane searchPane = new GridPane();
        searchPane.setHgap(10);
        searchPane.setVgap(10);
        searchPane.addRow(0, new Label("Journey Date:"), journeyDatePicker);
        searchPane.addRow(1, new Label("Origin:"), originComboBox);
        searchPane.addRow(2, new Label("Destination:"), destinationComboBox, searchFlightsBtn);

        HBox passengerPane = new HBox(15);
        VBox passengerForm = new VBox(10);
        passengerForm.getChildren().addAll(
                new Label("Passenger Details"),
                new GridPane() {{
                    setHgap(10);
                    setVgap(10);
                    addRow(0, new Label("Name:"), nameField);
                    addRow(1, new Label("Email:"), emailField);
                    addRow(2, new Label("Phone:"), phoneField);
                    add(addPassengerBtn, 1, 3);
                }}
        );
        passengerPane.getChildren().addAll(passengerForm, passengersTable);

        HBox paymentPane = new HBox(10);
        paymentPane.getChildren().addAll(
                new Label("Total Amount:"), totalAmountLabel,
                new Label("Payment Method:"), paymentMethodComboBox
        );

        root.getChildren().addAll(
                searchPane, flightsTable, selectFlightBtn,
                new Separator(), passengerPane, new Separator(),
                paymentPane, bookTicketBtn
        );


        searchFlightsBtn.setOnAction(e -> {
            if (journeyDatePicker.getValue() == null ||
                    originComboBox.getValue() == null ||
                    destinationComboBox.getValue() == null) {
                showAlert("Error", "Please select journey date, origin, and destination");
                return;
            }

            String origin = originComboBox.getValue().split(" - ")[0];
            String destination = destinationComboBox.getValue().split(" - ")[0];

            if (origin.equals(destination)) {
                showAlert("Error", "Origin and destination cannot be the same");
                return;
            }

            try {
                List<Flight> flights = backendService.searchFlights(
                        origin,
                        destination,
                        Date.valueOf(journeyDatePicker.getValue())
                );
                flightsTable.setItems(FXCollections.observableArrayList(flights));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to search flights: " + ex.getMessage());
            }
        });

        selectFlightBtn.setOnAction(e -> {
            Flight selected = flightsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                double price = selected.getPrice();
                totalAmountLabel.setText(String.format("₹%.2f", price * passengers.size()));
            }
        });

        addPassengerBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty() || emailField.getText().isEmpty() || phoneField.getText().isEmpty()) {
                showAlert("Error", "All passenger fields must be filled");
                return;
            }

            if (!isValidEmail(emailField.getText())) {
                showAlert("Error", "Invalid email format. It should be like username@gmail.com");
                return;
            }

            if (!isValidPhone(phoneField.getText())) {
                showAlert("Error", "Invalid phone number. It must contain exactly 10 digits.");
                return;
            }

            passengers.add(new Passenger(0, nameField.getText(), emailField.getText(), phoneField.getText()));
            passengersTable.setItems(passengers);


            Flight selected = flightsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                double price = selected.getPrice();
                totalAmountLabel.setText(String.format("₹%.2f", price * passengers.size()));
            }

            nameField.clear();
            emailField.clear();
            phoneField.clear();
        });

        bookTicketBtn.setOnAction(e -> {
            Flight selectedFlight = flightsTable.getSelectionModel().getSelectedItem();
            if (selectedFlight == null || passengers.isEmpty() ||
                    paymentMethodComboBox.getValue() == null) {
                showAlert("Error", "Please select flight, add passengers, and choose payment method");
                return;
            }

            try {
                String pnr = backendService.bookTicket(
                        selectedFlight.getFlightId(),
                        Date.valueOf(journeyDatePicker.getValue()),
                        passengers,
                        paymentMethodComboBox.getValue()
                );

                double totalAmount = selectedFlight.getPrice() * passengers.size();
                showAlert("Success", "Booking Successful!\nPNR: " + pnr +
                        "\nTotal Paid: ₹" + String.format("%.2f", totalAmount));

                passengers.clear();
                passengersTable.setItems(passengers);
                totalAmountLabel.setText("₹0.00");
                paymentMethodComboBox.getSelectionModel().clearSelection();

            } catch (Exception ex) {
                showAlert("Error", "Booking failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void showViewReservationsWindow() {
        Stage stage = new Stage();
        stage.setTitle("View Reservations");

        TableView<Reservation> table = new TableView<>();
        TableColumn<Reservation, String> pnrCol = new TableColumn<>("PNR");
        TableColumn<Reservation, Integer> passengerIdCol = new TableColumn<>("Passenger ID");
        TableColumn<Reservation, Integer> flightIdCol = new TableColumn<>("Flight ID");
        TableColumn<Reservation, String> seatCol = new TableColumn<>("Seat No");
        TableColumn<Reservation, Date> bookingDateCol = new TableColumn<>("Booking Date");
        TableColumn<Reservation, Date> journeyDateCol = new TableColumn<>("Journey Date");

        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        passengerIdCol.setCellValueFactory(new PropertyValueFactory<>("passengerId"));
        flightIdCol.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seatNo"));
        bookingDateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        journeyDateCol.setCellValueFactory(new PropertyValueFactory<>("journeyDate"));

        table.getColumns().addAll(pnrCol, passengerIdCol, flightIdCol, seatCol, bookingDateCol, journeyDateCol);

        try {
            List<Reservation> reservations = backendService.getAllReservations();
            table.setItems(FXCollections.observableArrayList(reservations));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load reservations: " + e.getMessage());
        }

        stage.setScene(new Scene(new VBox(table), 800, 600));
        stage.show();
    }

    private void showAddBaggageWindow() {
        Stage stage = new Stage();
        stage.setTitle("Add Baggage");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextField pnrField = new TextField();
        ComboBox<PassengerComboItem> passengerComboBox = new ComboBox<>();
        ToggleGroup studentToggleGroup = new ToggleGroup();
        RadioButton studentYes = new RadioButton("Yes");
        RadioButton studentNo = new RadioButton("No");
        studentYes.setToggleGroup(studentToggleGroup);
        studentNo.setToggleGroup(studentToggleGroup);
        studentNo.setSelected(true);

        ToggleGroup baggageTypeToggleGroup = new ToggleGroup();
        RadioButton cabinRadio = new RadioButton("Cabin");
        RadioButton checkedRadio = new RadioButton("Checked");
        cabinRadio.setToggleGroup(baggageTypeToggleGroup);
        checkedRadio.setToggleGroup(baggageTypeToggleGroup);
        checkedRadio.setSelected(true);

        TextField weightField = new TextField();
        Button searchBtn = new Button("Search");
        Button addBaggageBtn = new Button("Add Baggage");


        Label remainingWeightLabel = new Label("Remaining allowed weight: -");

        TableView<Baggage> baggageTable = new TableView<>();
        TableColumn<Baggage, Integer> idCol = new TableColumn<>("Baggage ID");
        TableColumn<Baggage, String> pnrCol = new TableColumn<>("PNR");
        TableColumn<Baggage, Double> weightCol = new TableColumn<>("Weight");
        TableColumn<Baggage, String> typeCol = new TableColumn<>("Type");

        idCol.setCellValueFactory(new PropertyValueFactory<>("baggageId"));
        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        baggageTable.getColumns().addAll(idCol, pnrCol, weightCol, typeCol);


        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("PNR:"), pnrField, searchBtn);
        form.addRow(1, new Label("Passenger:"), passengerComboBox);
        form.addRow(2, new Label("Student:"), new HBox(10, studentYes, studentNo));
        form.addRow(3, new Label("Baggage Type:"), new HBox(10, cabinRadio, checkedRadio));
        form.addRow(4, new Label("Weight (kg):"), weightField, addBaggageBtn);
        form.addRow(5, remainingWeightLabel);

        root.getChildren().addAll(form, baggageTable);


        searchBtn.setOnAction(e -> {
            String pnr = pnrField.getText().trim();
            if (pnr.isEmpty()) {
                showAlert("Error", "Please enter a PNR");
                return;
            }

            try {
                List<PassengerComboItem> passengers = backendService.getPassengersForPNR(pnr);
                if (passengers.isEmpty()) {
                    showAlert("Error", "PNR not found or invalid");
                    return;
                }

                passengerComboBox.getItems().clear();
                passengerComboBox.getItems().addAll(passengers);


                List<Baggage> baggageList = backendService.getBaggageForPNR(pnr);
                baggageTable.setItems(FXCollections.observableArrayList(baggageList));

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Database error: " + ex.getMessage());
            }
        });


        ChangeListener<Object> updateRemainingWeightListener = (obs, oldVal, newVal) -> {
            if (passengerComboBox.getValue() != null && baggageTypeToggleGroup.getSelectedToggle() != null) {
                try {
                    int passengerId = passengerComboBox.getValue().getId();
                    String pnr = pnrField.getText();
                    boolean isStudent = studentToggleGroup.getSelectedToggle() == studentYes;
                    String type = ((RadioButton)baggageTypeToggleGroup.getSelectedToggle()).getText();


                    double existingWeight = backendService.getTotalBaggageWeight(passengerId, pnr, type);

                    double maxWeight = type.equals("Cabin") ? 7.0 : (isStudent ? 25.0 : 15.0);
                    double remaining = maxWeight - existingWeight;

                    remainingWeightLabel.setText(String.format("Remaining allowed weight: %.1f kg (Max: %.1f kg, Used: %.1f kg)",
                            remaining, maxWeight, existingWeight));
                    remainingWeightLabel.setTextFill(remaining > 0 ? Color.GREEN : Color.RED);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    remainingWeightLabel.setText("Error calculating remaining weight");
                }
            }
        };

        passengerComboBox.valueProperty().addListener(updateRemainingWeightListener);
        baggageTypeToggleGroup.selectedToggleProperty().addListener(updateRemainingWeightListener);
        studentToggleGroup.selectedToggleProperty().addListener(updateRemainingWeightListener);

        addBaggageBtn.setOnAction(e -> {
            if (passengerComboBox.getValue() == null ||
                    baggageTypeToggleGroup.getSelectedToggle() == null ||
                    weightField.getText().isEmpty()) {
                showAlert("Error", "Please fill all fields");
                return;
            }

            int passengerId = passengerComboBox.getValue().getId();
            boolean isStudent = studentToggleGroup.getSelectedToggle() == studentYes;
            String type = ((RadioButton)baggageTypeToggleGroup.getSelectedToggle()).getText();
            double weight;

            try {
                weight = Double.parseDouble(weightField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid weight value");
                return;
            }

            String pnr = pnrField.getText();

            try {

                double existingWeight = backendService.getTotalBaggageWeight(passengerId, pnr, type);

                double maxWeight = type.equals("Cabin") ? 7.0 : (isStudent ? 25.0 : 15.0);
                double remaining = maxWeight - existingWeight;

                if (weight > remaining) {
                    showAlert("Error", String.format("Cannot add %.1f kg. Remaining allowed weight for %s baggage is %.1f kg (Max: %.1f kg, Used: %.1f kg)",
                            weight, type.toLowerCase(), remaining, maxWeight, existingWeight));
                    return;
                }

                backendService.addBaggage(passengerId, pnr, weight, type);
                showAlert("Success", "Baggage added successfully");
                searchBtn.fire();
                weightField.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Database error: " + ex.getMessage());
            }
        });

        stage.setScene(new Scene(root, 600, 400));
        stage.show();
    }

    public static class PassengerComboItem {
        private int id;
        private String name;

        public PassengerComboItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() { return id; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return id + " - " + name;
        }
    }

    private void showViewFlightsWindow() {
        Stage stage = new Stage();
        stage.setTitle("View Flights");

        TableView<Flight> table = new TableView<>();
        TableColumn<Flight, Integer> flightIdCol = new TableColumn<>("Flight ID");
        TableColumn<Flight, String> airlineCol = new TableColumn<>("Airline");
        TableColumn<Flight, String> originCol = new TableColumn<>("Origin");
        TableColumn<Flight, String> destCol = new TableColumn<>("Destination");
        TableColumn<Flight, Time> departureCol = new TableColumn<>("Departure");
        TableColumn<Flight, Time> arrivalCol = new TableColumn<>("Arrival");

        flightIdCol.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        airlineCol.setCellValueFactory(new PropertyValueFactory<>("airline"));
        originCol.setCellValueFactory(new PropertyValueFactory<>("origin"));
        destCol.setCellValueFactory(new PropertyValueFactory<>("destination"));
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        arrivalCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        table.getColumns().addAll(flightIdCol, airlineCol, originCol, destCol, departureCol, arrivalCol);

        try {
            List<Flight> flights = backendService.getAllFlights();
            table.setItems(FXCollections.observableArrayList(flights));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load flights: " + e.getMessage());
        }

        stage.setScene(new Scene(new VBox(table), 800, 600));
        stage.show();
    }

    private void showViewPassengersWindow() {
        Stage stage = new Stage();
        stage.setTitle("View Passengers");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        ComboBox<Integer> flightComboBox = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        Button searchBtn = new Button("Search");
        TableView<Passenger> table = new TableView<>();
        TableColumn<Passenger, String> nameCol = new TableColumn<>("Name");
        TableColumn<Passenger, String> emailCol = new TableColumn<>("Email");
        TableColumn<Passenger, String> phoneCol = new TableColumn<>("Phone");
        TableColumn<Passenger, String> seatCol = new TableColumn<>("Seat");
        TableColumn<Passenger, String> pnrCol = new TableColumn<>("PNR");

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        seatCol.setCellValueFactory(cellData -> {
            return Bindings.createStringBinding(() ->
                    backendService.getSeatNumberForPassenger(cellData.getValue().getPassengerId()));
        });
        pnrCol.setCellValueFactory(cellData -> {
            return Bindings.createStringBinding(() ->
                    backendService.getPnrForPassenger(cellData.getValue().getPassengerId()));
        });

        table.getColumns().addAll(nameCol, emailCol, phoneCol, seatCol, pnrCol);


        try {
            List<Integer> flightIds = backendService.getAllFlightIds();
            flightComboBox.getItems().addAll(flightIds);
        } catch (Exception e) {
            e.printStackTrace();
        }

        searchBtn.setOnAction(e -> {
            if (flightComboBox.getValue() == null || datePicker.getValue() == null) {
                showAlert("Error", "Please select flight and date");
                return;
            }

            try {
                List<Passenger> passengers = backendService.getPassengersForFlight(
                        flightComboBox.getValue(),
                        Date.valueOf(datePicker.getValue())
                );
                table.setItems(FXCollections.observableArrayList(passengers));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to load passengers: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                new HBox(10, new Label("Flight ID:"), flightComboBox),
                new HBox(10, new Label("Journey Date:"), datePicker, searchBtn),
                table
        );

        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void showViewPaymentsWindow() {
        Stage stage = new Stage();
        stage.setTitle("View Payments");

        TableView<Payment> table = new TableView<>();
        TableColumn<Payment, Integer> paymentIdCol = new TableColumn<>("Payment ID");
        TableColumn<Payment, String> pnrCol = new TableColumn<>("PNR");
        TableColumn<Payment, Double> amountCol = new TableColumn<>("Amount");
        TableColumn<Payment, String> methodCol = new TableColumn<>("Method");
        TableColumn<Payment, Timestamp> dateCol = new TableColumn<>("Transaction Date");

        paymentIdCol.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        methodCol.setCellValueFactory(new PropertyValueFactory<>("method"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("transactionDate"));

        table.getColumns().addAll(paymentIdCol, pnrCol, amountCol, methodCol, dateCol);

        try {
            List<Payment> payments = backendService.getAllPayments();
            table.setItems(FXCollections.observableArrayList(payments));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load payments: " + e.getMessage());
        }

        stage.setScene(new Scene(new VBox(table), 800, 600));
        stage.show();
    }

    private void showCancelTicketWindow() {
        Stage stage = new Stage();
        stage.setTitle("Cancel Ticket");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextField pnrField = new TextField();
        Button searchBtn = new Button("Search");
        Button cancelBtn = new Button("Cancel Ticket");
        TableView<Reservation> table = new TableView<>();
        TableColumn<Reservation, String> pnrCol = new TableColumn<>("PNR");
        TableColumn<Reservation, Integer> passengerIdCol = new TableColumn<>("Passenger ID");
        TableColumn<Reservation, Integer> flightIdCol = new TableColumn<>("Flight ID");
        TableColumn<Reservation, String> seatCol = new TableColumn<>("Seat No");
        TableColumn<Reservation, Date> bookingDateCol = new TableColumn<>("Booking Date");
        TableColumn<Reservation, Date> journeyDateCol = new TableColumn<>("Journey Date");

        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        passengerIdCol.setCellValueFactory(new PropertyValueFactory<>("passengerId"));
        flightIdCol.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        seatCol.setCellValueFactory(new PropertyValueFactory<>("seatNo"));
        bookingDateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        journeyDateCol.setCellValueFactory(new PropertyValueFactory<>("journeyDate"));

        table.getColumns().addAll(pnrCol, passengerIdCol, flightIdCol, seatCol, bookingDateCol, journeyDateCol);

        searchBtn.setOnAction(e -> {
            String pnr = pnrField.getText().trim();
            if (pnr.isEmpty()) {
                showAlert("Error", "Please enter a PNR");
                return;
            }

            try {
                List<Reservation> reservations = backendService.getReservationsByPNR(pnr);
                table.setItems(FXCollections.observableArrayList(reservations));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to search reservations: " + ex.getMessage());
            }
        });

        cancelBtn.setOnAction(e -> {
            String pnr = pnrField.getText().trim();
            if (pnr.isEmpty()) {
                showAlert("Error", "Please enter a PNR");
                return;
            }

            try {
                backendService.cancelTicket(pnr);
                showAlert("Success", "Ticket cancellation successful for PNR: " + pnr);
                searchBtn.fire();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Cancellation failed: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                new HBox(10, new Label("PNR:"), pnrField, searchBtn),
                table,
                cancelBtn
        );

        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void showUpdatePassengerWindow() {
        Stage stage = new Stage();
        stage.setTitle("Update Passenger Details");


        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.setAlignment(Pos.TOP_CENTER);


        HBox searchBox = new HBox(10);
        TextField pnrField = new TextField();
        pnrField.setPromptText("Enter PNR");
        Button searchBtn = new Button("Search");


        ComboBox<Map.Entry<Integer, String>> passengerComboBox = new ComboBox<>();
        passengerComboBox.setPromptText("Select Passenger");


        passengerComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Map.Entry<Integer, String> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getKey() + " - " + item.getValue());
            }
        });
        passengerComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Map.Entry<Integer, String> item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getKey() + " - " + item.getValue());
            }
        });


        ToggleGroup detailGroup = new ToggleGroup();
        RadioButton emailRadio = new RadioButton("Email");
        RadioButton phoneRadio = new RadioButton("Phone");
        emailRadio.setToggleGroup(detailGroup);
        phoneRadio.setToggleGroup(detailGroup);
        emailRadio.setSelected(true);

        TextField newValueField = new TextField();
        newValueField.setPromptText("Enter new value");
        newValueField.setPrefWidth(100);
        newValueField.setMaxWidth(100);

        Button updateBtn = new Button("Update");


        searchBtn.setOnAction(e -> {
            String pnr = pnrField.getText().trim();
            if (pnr.isEmpty()) {
                showAlert("Error", "Please enter PNR");
                return;
            }

            try {
                Map<Integer, String> passengerMap = backendService.getPassengersForPNRUPDATE(pnr);
                passengerComboBox.getItems().clear();
                passengerComboBox.getItems().addAll(passengerMap.entrySet());

                if (passengerComboBox.getItems().isEmpty()) {
                    showAlert("Info", "No passengers found for PNR: " + pnr);
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to load passengers: " + ex.getMessage());
            }
        });


        updateBtn.setOnAction(e -> {
            Map.Entry<Integer, String> selectedEntry = passengerComboBox.getValue();
            if (selectedEntry == null) {
                showAlert("Error", "Please select a passenger");
                return;
            }

            if (newValueField.getText().trim().isEmpty()) {
                showAlert("Error", "Please enter a new value");
                return;
            }

            int passengerId = selectedEntry.getKey();
            String detailType = ((RadioButton) detailGroup.getSelectedToggle()).getText();
            String newValue = newValueField.getText().trim();

            try {
                backendService.updatePassengerDetails(passengerId, detailType, newValue);
                showAlert("Success", "Details updated successfully");
                newValueField.clear();
            } catch (IllegalStateException ex) {
                showAlert("Info", ex.getMessage());
            } catch (Exception ex) {
                showAlert("Error", "Update failed: " + ex.getMessage());
            }
        });


        searchBox.getChildren().addAll(new Label("PNR:"), pnrField, searchBtn);
        HBox radioBox = new HBox(10, emailRadio, phoneRadio);

        root.getChildren().addAll(
                searchBox,
                new Label("Passenger:"),
                passengerComboBox,
                new Label("Update Field:"),
                radioBox,
                new Label("New Value:"),
                newValueField,
                updateBtn
        );


        stage.setScene(new Scene(root, 400, 320));
        stage.show();
    }

    private void showCheckAvailabilityWindow() {
        Stage stage = new Stage();
        stage.setTitle("Check Flight Availability");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        ComboBox<String> originComboBox = new ComboBox<>();
        ComboBox<String> destinationComboBox = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        Button searchBtn = new Button("Search");
        TableView<FlightAvailability> table = new TableView<>();
        TableColumn<FlightAvailability, Integer> flightIdCol = new TableColumn<>("Flight ID");
        TableColumn<FlightAvailability, String> airlineCol = new TableColumn<>("Airline");
        TableColumn<FlightAvailability, String> routeCol = new TableColumn<>("Route");
        TableColumn<FlightAvailability, Time> departureCol = new TableColumn<>("Departure");
        TableColumn<FlightAvailability, Integer> seatsCol = new TableColumn<>("Available Seats");

        flightIdCol.setCellValueFactory(new PropertyValueFactory<>("flightId"));
        airlineCol.setCellValueFactory(new PropertyValueFactory<>("airline"));
        routeCol.setCellValueFactory(new PropertyValueFactory<>("route"));
        departureCol.setCellValueFactory(new PropertyValueFactory<>("departureTime"));
        seatsCol.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));

        table.getColumns().addAll(flightIdCol, airlineCol, routeCol, departureCol, seatsCol);

        initializeAirportComboBoxes(originComboBox, destinationComboBox);

        searchBtn.setOnAction(e -> {
            if (originComboBox.getValue() == null ||
                    destinationComboBox.getValue() == null ||
                    datePicker.getValue() == null) {
                showAlert("Error", "Please select origin, destination, and date");
                return;
            }

            String origin = originComboBox.getValue().split(" - ")[0];
            String destination = destinationComboBox.getValue().split(" - ")[0];

            if (origin.equals(destination)) {
                showAlert("Error", "Origin and destination cannot be the same");
                return;
            }

            try {
                List<FlightAvailability> flights = backendService.checkFlightAvailability(
                        origin,
                        destination,
                        Date.valueOf(datePicker.getValue())
                );
                table.setItems(FXCollections.observableArrayList(flights));
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to check availability: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(
                new GridPane() {{
                    setHgap(10);
                    setVgap(10);
                    addRow(0, new Label("Origin:"), originComboBox);
                    addRow(1, new Label("Destination:"), destinationComboBox);
                    addRow(2, new Label("Date:"), datePicker, searchBtn);
                }},
                table
        );

        stage.setScene(new Scene(root, 700, 500));
        stage.show();
    }

    private void showAllBaggageWindow() {
        Stage stage = new Stage();
        stage.setTitle("All Baggage");

        TableView<Baggage> table = new TableView<>();
        TableColumn<Baggage, Integer> idCol = new TableColumn<>("Baggage ID");
        TableColumn<Baggage, Integer> passengerIdCol = new TableColumn<>("Passenger ID");
        TableColumn<Baggage, String> pnrCol = new TableColumn<>("PNR");
        TableColumn<Baggage, Double> weightCol = new TableColumn<>("Weight");
        TableColumn<Baggage, String> typeCol = new TableColumn<>("Type");

        idCol.setCellValueFactory(new PropertyValueFactory<>("baggageId"));
        passengerIdCol.setCellValueFactory(new PropertyValueFactory<>("passengerId"));
        pnrCol.setCellValueFactory(new PropertyValueFactory<>("pnr"));
        weightCol.setCellValueFactory(new PropertyValueFactory<>("weight"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        table.getColumns().addAll(idCol, passengerIdCol, pnrCol, weightCol, typeCol);

        try {
            List<Baggage> baggageList = backendService.getAllBaggage();
            table.setItems(FXCollections.observableArrayList(baggageList));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load baggage: " + e.getMessage());
        }

        stage.setScene(new Scene(new VBox(table), 800, 600));
        stage.show();
    }

    private void initializeAirportComboBoxes(ComboBox<String> origin, ComboBox<String> destination) {
        try {
            List<String> airports = backendService.getAllAirports();
            origin.getItems().addAll(airports);
            destination.getItems().addAll(airports);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.com$";
        return Pattern.matches(emailRegex, email);
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static class Passenger {
        private int passengerId;
        private String name;
        private String email;
        private String phone;

        public Passenger(int passengerId, String name, String email, String phone) {
            this.passengerId = passengerId;
            this.name = name;
            this.email = email;
            this.phone = phone;
        }

        public int getPassengerId() { return passengerId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
    }

    public static class Flight {
        private int flightId;
        private String airline;
        private String origin;
        private String destination;
        private Time departureTime;
        private Time arrivalTime;
        private double price;

        public Flight(int flightId, String airline, String origin, String destination,
                      Time departureTime, Time arrivalTime, double price) {
            this.flightId = flightId;
            this.airline = airline;
            this.origin = origin;
            this.destination = destination;
            this.departureTime = departureTime;
            this.arrivalTime = arrivalTime;
            this.price = price;
        }

        public int getFlightId() { return flightId; }
        public String getAirline() { return airline; }
        public String getOrigin() { return origin; }
        public String getDestination() { return destination; }
        public Time getDepartureTime() { return departureTime; }
        public Time getArrivalTime() { return arrivalTime; }
        public double getPrice() { return price; }
    }

    public static class Reservation {
        private String pnr;
        private int passengerId;
        private int flightId;
        private String seatNo;
        private Date bookingDate;
        private Date journeyDate;

        public Reservation(String pnr, int passengerId, int flightId,
                           String seatNo, Date bookingDate, Date journeyDate) {
            this.pnr = pnr;
            this.passengerId = passengerId;
            this.flightId = flightId;
            this.seatNo = seatNo;
            this.bookingDate = bookingDate;
            this.journeyDate = journeyDate;
        }

        public String getPnr() { return pnr; }
        public int getPassengerId() { return passengerId; }
        public int getFlightId() { return flightId; }
        public String getSeatNo() { return seatNo; }
        public Date getBookingDate() { return bookingDate; }
        public Date getJourneyDate() { return journeyDate; }
    }

    public static class Payment {
        private int paymentId;
        private String pnr;
        private double amount;
        private String method;
        private Timestamp transactionDate;

        public Payment(int paymentId, String pnr, double amount, String method, Timestamp transactionDate) {
            this.paymentId = paymentId;
            this.pnr = pnr;
            this.amount = amount;
            this.method = method;
            this.transactionDate = transactionDate;
        }

        public int getPaymentId() { return paymentId; }
        public String getPnr() { return pnr; }
        public double getAmount() { return amount; }
        public String getMethod() { return method; }
        public Timestamp getTransactionDate() { return transactionDate; }
    }

    public static class Baggage {
        private int baggageId;
        private int passengerId;
        private String pnr;
        private double weight;
        private String type;

        public Baggage(int baggageId, int passengerId, String pnr, double weight, String type) {
            this.baggageId = baggageId;
            this.passengerId = passengerId;
            this.pnr = pnr;
            this.weight = weight;
            this.type = type;
        }

        public Baggage(int baggageId, String pnr, double weight, String type) {
            this(baggageId, 0, pnr, weight, type);
        }

        public int getBaggageId() { return baggageId; }
        public int getPassengerId() { return passengerId; }
        public String getPnr() { return pnr; }
        public double getWeight() { return weight; }
        public String getType() { return type; }
    }

    public static class FlightAvailability {
        private int flightId;
        private String airline;
        private String route;
        private Time departureTime;
        private int availableSeats;

        public FlightAvailability(int flightId, String airline, String route, Time departureTime, int availableSeats) {
            this.flightId = flightId;
            this.airline = airline;
            this.route = route;
            this.departureTime = departureTime;
            this.availableSeats = availableSeats;
        }

        public int getFlightId() { return flightId; }
        public String getAirline() { return airline; }
        public String getRoute() { return route; }
        public Time getDepartureTime() { return departureTime; }
        public int getAvailableSeats() { return availableSeats; }
    }
}