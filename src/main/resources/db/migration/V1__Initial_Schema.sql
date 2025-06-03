-- Flyway Migration Script: V1__Initial_Schema.sql
-- Description: Creates the initial database schema for the Bus Booking application (PostgreSQL).

-- Table: users
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                       first_name VARCHAR(50) NOT NULL,              -- User's first name
                       last_name VARCHAR(50) NOT NULL,               -- User's last name
                       email VARCHAR(100) NOT NULL UNIQUE,           -- User's email, must be unique
                       password VARCHAR(120) NOT NULL,               -- Hashed password
                       phone_number VARCHAR(10) NOT NULL,            -- User's phone number
                       age INTEGER NOT NULL,                         -- User's age
                       gender VARCHAR(255) NOT NULL,                 -- User's gender
                       date_of_birth DATE NOT NULL,                  -- User's date of birth
                       role VARCHAR(20) NOT NULL,                    -- User's role ('ROLE_USER' or 'ROLE_ADMIN')
                       CONSTRAINT chk_users_age CHECK (age >= 1),    -- Ensure age is positive
                       CONSTRAINT chk_users_role CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN')) -- Ensure valid role
);

-- Table: buses
CREATE TABLE buses (
                       id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                       bus_number VARCHAR(255) NOT NULL UNIQUE,      -- Unique identifier for the bus
                       operator_name VARCHAR(255) NOT NULL,          -- Name of the bus operator
                       bus_type VARCHAR(255) NOT NULL,               -- Type of bus (e.g., AC Sleeper)
                       total_seats INTEGER NOT NULL,                 -- Total number of seats on the bus
                       seat_layout VARCHAR(500),                     -- String representation of the seat layout
                       CONSTRAINT chk_buses_total_seats CHECK (total_seats >= 1) -- Ensure at least one seat
);

-- Table: routes
CREATE TABLE routes (
                        id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                        origin VARCHAR(255) NOT NULL,                 -- Starting location of the route
                        destination VARCHAR(255) NOT NULL             -- Ending location of the route
);

-- Table: scheduled_trips
CREATE TABLE scheduled_trips (
                                 id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                                 bus_id BIGINT NOT NULL REFERENCES buses(id),  -- Foreign key referencing the bus
                                 route_id BIGINT NOT NULL REFERENCES routes(id),-- Foreign key referencing the route
                                 departure_date DATE NOT NULL,                 -- Date of departure
                                 departure_time TIME NOT NULL,                 -- Time of departure
                                 arrival_time TIME NOT NULL,                   -- Time of arrival
                                 fare NUMERIC(19, 2) NOT NULL,                 -- Cost of the trip per seat
                                 available_seats INTEGER NOT NULL,             -- Number of currently available seats
                                 CONSTRAINT chk_scheduled_trips_available_seats CHECK (available_seats >= 0) -- Ensure non-negative seats
);

-- Table: bookings
CREATE TABLE bookings (
                          id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                          booking_time TIMESTAMP NOT NULL,              -- Timestamp when the booking was made (use TIMESTAMP for datetime)
                          number_of_seats INTEGER NOT NULL,             -- Number of seats booked in this transaction
                          status VARCHAR(30) NOT NULL,                  -- Status of the booking (e.g., PENDING, CONFIRMED)
                          total_fare NUMERIC(19, 2) NOT NULL,           -- Total cost for this booking
                          trip_id BIGINT NOT NULL REFERENCES scheduled_trips(id), -- Foreign key referencing the scheduled trip
                          user_id BIGINT NOT NULL REFERENCES users(id), -- Foreign key referencing the user who booked
                          CONSTRAINT chk_bookings_number_of_seats CHECK (number_of_seats >= 1) -- Ensure at least one seat booked
);

-- Table: passengers
CREATE TABLE passengers (
                            id BIGSERIAL PRIMARY KEY,                     -- Auto-incrementing 64-bit integer primary key
                            age INTEGER NOT NULL,                         -- Age of the passenger
                            gender VARCHAR(255) NOT NULL,                 -- Gender of the passenger
                            name VARCHAR(255) NOT NULL,                   -- Name of the passenger
                            seat_number VARCHAR(255) NOT NULL,            -- Assigned seat number for this passenger
                            booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE, -- Foreign key referencing the booking, cascade delete passengers if booking is deleted
                            CONSTRAINT chk_passengers_age CHECK (age >= 1) -- Ensure age is positive
);

-- Table: bus_amenities (Join table for Bus amenities)
CREATE TABLE bus_amenities (
                               bus_id BIGINT NOT NULL REFERENCES buses(id) ON DELETE CASCADE, -- If bus is deleted, remove amenities
                               amenity VARCHAR(255) NOT NULL,                -- Name of the amenity (e.g., WiFi, AC)
                               PRIMARY KEY (bus_id, amenity)                 -- Ensure amenity is unique per bus
);

-- Table: trip_seat_status (Join table for ScheduledTrip seat status map)
CREATE TABLE trip_seat_status (
                                  trip_id BIGINT NOT NULL REFERENCES scheduled_trips(id) ON DELETE CASCADE, -- If trip is deleted, remove seat statuses
                                  seat_number VARCHAR(255) NOT NULL,           -- The seat identifier (map key)
                                  status VARCHAR(20) NOT NULL,                 -- The status of the seat (map value)
                                  PRIMARY KEY (trip_id, seat_number),          -- Each seat number must be unique per trip
                                  CONSTRAINT chk_trip_seat_status_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'LOCKED', 'UNAVAILABLE')) -- Ensure valid status
);

-- Optional: Add Indexes for frequently queried columns (improves performance)
-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_buses_bus_number ON buses(bus_number);
-- CREATE INDEX idx_routes_origin_destination ON routes(origin, destination);
-- CREATE INDEX idx_scheduled_trips_route_date ON scheduled_trips(route_id, departure_date);
-- CREATE INDEX idx_bookings_user_id ON bookings(user_id);
-- CREATE INDEX idx_bookings_trip_id ON bookings(trip_id);
