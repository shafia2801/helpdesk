# 1. Use Maven image to build the app
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies first (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build JAR
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Use a smaller JDK image to run the app
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy only the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (matches application.properties server.port=8080)
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]