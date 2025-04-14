# 🚌 BusApp - Online Bus Ticket Booking System ✨

![Java](https://img.shields.io/badge/Java-17-blue) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green) ![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-blueviolet) ![JPA/Hibernate](https://img.shields.io/badge/JPA-Hibernate-orange) ![MySQL](https://img.shields.io/badge/MySQL-DB-lightgrey) ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-Frontend-yellowgreen) ![Stripe](https://img.shields.io/badge/Stripe-Payments-purple)

## Overview

Welcome to BusApp! This is a full-stack web application designed to streamline the process of searching, booking, and managing bus tickets online. Built with modern Java technologies, it provides distinct interfaces for both regular users seeking travel and administrators managing the service.

## ✨ Key Features

* **👤 User Management:** Secure JWT-based authentication (Login/Registration) with distinct User & Admin roles. Profile viewing, editing, and password changes.
* **🚌 Admin Controls:** Comprehensive CRUD operations for:
    * Buses (including details, amenities, seat layouts)
    * Routes (origin/destination pairs)
    * Scheduled Trips (linking buses, routes, dates, times, fares)
* **🔍 Trip Search:** Users can easily search for available bus trips based on origin, destination, and date.
* **💺 Seat Selection:** Interactive (basic) seat selection interface showing available/booked/locked seats.
* **🔒 Seat Locking:** Temporary seat locking during the booking process with automatic expiry for abandoned bookings.
* **🧾 Booking Flow:** Multi-step booking process including passenger details entry.
* **💳 Stripe Payment:** Secure payment processing using Stripe Payment Intents. Asynchronous confirmation via Stripe Webhooks.
* **📧 Email Confirmation:** Automated email ticket/confirmation sent upon successful payment (using Thymeleaf templates).
* **📜 Booking History:** Users can view their past and current bookings with status updates.
* **🛠️ Admin Booking View:** Admins can view a list of all bookings made across the system.
* **📄 API Documentation:** Interactive API documentation via Swagger UI.
* **🛡️ Error Handling:** Centralized exception handling for consistent API error responses.
* **🧪 Unit Tested:** Service layer logic covered by JUnit 5 & Mockito tests.

## ⚙️ Tech Stack

* **Backend:** Java 17, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA (Hibernate), Lombok
* **Database:** MySQL
* **Frontend:** Thymeleaf, HTML5, CSS3, JavaScript (Fetch API)
* **Payments:** Stripe Java SDK
* **Email:** Spring Boot Mail Starter + Thymeleaf + Gmail SMTP
* **API Docs:** SpringDoc OpenAPI
* **Build:** Maven
* **Testing:** JUnit 5, Mockito

## 🔑 Prerequisites

* Java Development Kit (JDK) 17+
* Apache Maven 3.6+
* Git client
* MySQL Server (running locally or accessible)
* Stripe Account (Test keys: Publishable Key, Secret Key, Webhook Secret)
* Gmail Account + **App Password** (for `spring.mail.password` if using Gmail)

## 🚀 Getting Started

1.  **Clone:** `git clone <https://github.com/arunprakashxavier/bus-booking-app>`
2.  **Database:**
    * Start MySQL server.
    * Create a database named `busapp`.
    * Ensure a MySQL user exists with privileges on `busapp` (e.g., `root`/`your_password`).
3.  **Configure Secrets (Environment Variables):**
    * **IMPORTANT:** Since `application.properties` is excluded by `.gitignore`, set these environment variables in your IDE Run Configuration or OS:
        * `SPRING_DATASOURCE_PASSWORD`: Your MySQL password
        * `STRIPE_SECRET_KEY`: Your Stripe Secret Key (`sk_test_...`)
        * `STRIPE_PUBLISHABLE_KEY`: Your Stripe Publishable Key (`pk_test_...`)
        * `STRIPE_WEBHOOK_SECRET`: Your Stripe Webhook Secret (`whsec_...` from `stripe listen`)
        * `APP_JWTSECRET`: Your chosen JWT secret (long, random string)
        * `SPRING_MAIL_USERNAME`: Your Gmail address
        * `SPRING_MAIL_PASSWORD`: Your Gmail **App Password**
4.  **Build:** Navigate to the project root and run:
    ```bash
    mvn clean install
    ```
5.  **Run:**
    * From IDE: Run the `BusappApplication.java` main method.
    * From command line: `mvn spring-boot:run`

    The application will be accessible at `http://localhost:8080`.

## API Endpoints

API documentation is available via Swagger UI when the application is running:
[`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html)

## Usage Guide

* **Access:** Open `http://localhost:8080` (redirects to `/login`).
* **User:** Register an account or log in. Use the dashboard to search for trips (e.g., Chennai -> Kanyakumari or Trichy -> Banglore for dates you added trips for). Select seats, enter passenger details, proceed to payment (use Stripe test card numbers), and check `/booking-history`. Edit profile via `/profile`.
* **Admin:** Log in with an admin account (you may need to create one, e.g., by updating a registered user's role directly in the database to `ROLE_ADMIN`). Access admin functions via the `/admin/...` paths in the navbar to manage buses, routes, scheduled trips, and view all bookings.

## 🐛 Known Issues / Troubleshooting

* **Email in Spam:** Emails sent via the basic Gmail SMTP setup might land in the spam folder. Mark them as "Not Spam" during testing. For production, use a dedicated transactional email service (e.g., SendGrid, AWS SES).

## 💡 Future Ideas

* Implement Integration Tests (`@SpringBootTest`, `@WebMvcTest`).
* Refine CSS and UI components.
* Replace manual DTO mapping (e.g., with MapStruct).
* Implement visual seat layout selection.
* Add Pagination for lists (Admin pages, User History).
* Implement Booking Cancellation.
* Refactor `PaymentServiceImpl` to improve testability of `createPaymentIntent`.
* Confirm `@Value` works for webhook secret loading and remove `Environment` injection if preferred.

---

