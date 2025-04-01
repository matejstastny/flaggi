FROM openjdk:8-jre-slim

WORKDIR /app

COPY *.jar /app/

CMD ["sh", "-c", "exec java -jar $(ls /app/*.jar)"]
