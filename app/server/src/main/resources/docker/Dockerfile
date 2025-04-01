FROM openjdk:8-jre-slim

WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} /app/server.jar

CMD ["sh", "-c", "exec java -jar /app/server.jar"]
