# Use Java 8
FROM openjdk:8-jdk

# Set the working directory in the container to the server directory
WORKDIR /app/server

# Install wget, unzip (needed to install Gradle)
RUN apt-get update && apt-get install -y wget unzip

# Install Gradle 8.10.2
RUN wget https://services.gradle.org/distributions/gradle-8.10.2-bin.zip -P /tmp \
    && unzip /tmp/gradle-8.10.2-bin.zip -d /opt \
    && ln -s /opt/gradle-8.10.2/bin/gradle /usr/bin/gradle

# Copy the server project files & run script into the container
COPY server /app/server
COPY scripts/run.sh /app/scripts/run.sh

# Make the run-server.sh script executable
RUN chmod +x /app/scripts/run.sh

# Expose the ports for the server
EXPOSE 54321 54322

# Run the run-server.sh script
CMD ["/app/scripts/run.sh", "server", "--nodiet"]
