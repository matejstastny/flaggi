# Stage 1: Build
FROM gradle:8.0.2-jdk17 AS builder
WORKDIR /app

# Copy the entire project into the container
COPY . .

# Build only the 'server' module
RUN gradle :server:shadowjar

# Stage 2: Runtime
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file for the 'server' module
COPY --from=builder /app/server/build/libs/Flaggi-server.jar server.jar

# Expose server ports
EXPOSE 54321 54322

# Set the default command to run the server
CMD ["java", "-jar", "server.jar", "--nodiet"]
