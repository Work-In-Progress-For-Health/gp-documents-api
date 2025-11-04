# Use Debian as the base image with OpenJDK 18 installed
FROM openjdk:18-ea-11-jdk-bullseye

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR into the container
COPY target/gp-practice-documents-1.0.0.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Command to run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]

