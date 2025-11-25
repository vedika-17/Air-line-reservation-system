# **Airline Booking System (Java + MySQL + JavaFX)**

This project is a **Java-based Airline Booking System** developed for academic submission.  
It provides features for managing **flights, airports, reservations, passengers, payments, baggage, and seat availability** with a complete **MySQL database backend** and **JavaFX UI**.

---

## **Project Features**

### **1. Complete Database Management**
- Fully normalized relational database  
- All major airline tables included  
- Proper primary and foreign keys  
- Cascading updates and deletes  
- Sample data for airports and flights  

---

### **2. JavaFX Frontend Interface**
- User-friendly airline booking dashboard  
- Styled using CSS  
- Seat booking and search interface  
- Fully interactive UI  

---

### **3. Java Backend (JDBC Integration)**
- Connects JavaFX to MySQL  
- Executes queries and updates  
- Handles booking logic, seat availability, payments, and baggage  

---

### **4. Reservation System**
- Generates unique PNR  
- Books seats for a selected journey date  
- Prevents double-booking with unique constraints  
- Stores passenger details and booking date  

---

### **5. Flight Availability Tracking**
- Tracks available seats for each flight on each date  
- Automatically updates seats during booking  
- Ensures availability cannot go below zero  

---

### **6. Payment Module**
- Stores payment details  
- Enforces **1 payment per PNR** rule  
- Stores amount, method, and transaction time  

---

### **7. Baggage Management**
- Stores baggage weight and type  
- Linked to passenger and PNR  
- Protects against invalid baggage entries  

---

## **Database Entities**
- **AIRPORT**  
- **FLIGHTS**  
- **FLIGHT_AVAILABILITY**  
- **PASSENGERS**  
- **RESERVATION**  
- **PAYMENT**  
- **BAGGAGE**

---



---

## **Technologies Used**
- Java  
- JavaFX  
- CSS  
- JDBC  
- MySQL  
- MySQL Workbench  

---

## **Setup Requirements**

### **1. Install Required Software**
- Java 11 or above  
- JavaFX SDK 17+  
- MySQL Server 8.0+  
- MySQL Connector/J  
- IntelliJ IDEA / Eclipse / VS Code  

---

## **How to Run the Project**

### **1. Import MySQL Database**
- Create database **airline_db**  
- Import **project.sql**

### **2. Configure JavaFX**
Add VM options:
--module-path "C:\javafx-sdk-17\lib" --add-modules javafx.controls,javafx.fxml


### **3. Add MySQL Connector/J**
- Include the `.jar` file in project libraries

### **4. Update Database Credentials**
In backend.java:
java
String url = "jdbc:mysql://localhost:3306/airline_db";
String user = "root";
String password = "yourpassword";

**5. Run the Application**

Run:

AirlineBookingSystemFrontend.java


The JavaFX UI will open.

**Report and ER Diagram**

**Includes ER diagram image**

SQL schema file

Java backend and frontend source code
