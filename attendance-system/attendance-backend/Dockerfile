# Use OpenJDK 17 Alpine as base
FROM eclipse-temurin:17-jdk-alpine

# Install MySQL and tools
RUN apk add --no-cache mysql mysql-client bash

# Set environment variables for MySQL
ENV MYSQL_ROOT_PASSWORD=root
ENV MYSQL_DATABASE=attendance_db

# Create MySQL data directory
RUN mkdir -p /var/lib/mysql /run/mysqld

# Working directory
WORKDIR /app

# Copy the Spring Boot JAR
COPY target/attendance-backend-1.0.0.jar app.jar

# Copy frontend (static files) to serve via Spring Boot
COPY src/main/resources/static /app/static

# Expose Spring Boot (9090) and MySQL (3306) ports
EXPOSE 9090 3306

# Start MySQL first, wait 10 seconds, then start Spring Boot
CMD mysqld_safe --datadir=/var/lib/mysql & \
    sleep 10 && \
    echo "✅ MySQL started. Launching Spring Boot..." && \
    java -jar app.jar

