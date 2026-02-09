FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app
COPY services/imis-connect/pom.xml .
COPY services/imis-connect/src ./src

RUN apk add --no-cache maven && mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/imis-connect-*.jar imis-connect.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "imis-connect.jar"]
