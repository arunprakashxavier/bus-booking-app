# Use an official OpenJDK 17 runtime as a parent image
# Choose one with Maven pre-installed for easier build steps
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project definition first to leverage Docker cache for dependencies
COPY pom.xml ./
# Download dependencies (optional but speeds up subsequent builds if pom.xml hasn't changed)
# RUN mvn dependency:go-offline

# Copy the rest of the application source code
COPY src ./src

# Package the application using Maven, skipping tests
# This creates the JAR file in /app/target/
RUN mvn clean package -DskipTests

# --- Second Stage: Create the final, smaller runtime image ---
# Use a smaller JRE image for the final container
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Copy only the built JAR file from the 'build' stage
COPY --from=build /app/target/busapp-0.0.1-SNAPSHOT.jar ./app.jar

# Expose the port the application runs on
EXPOSE 8080

# Define the command to run the application
ENTRYPOINT ["java", "-jar", "./app.jar"]