# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY services/eip-hcwathome-openmrs-rabbitmq/pom.xml .
COPY services/eip-hcwathome-openmrs-rabbitmq/src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY services/eip-hcwathome-openmrs-rabbitmq/docker/entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Create log directory
RUN mkdir -p /var/log/eip && chmod 777 /var/log/eip

ENTRYPOINT ["./entrypoint.sh"]
