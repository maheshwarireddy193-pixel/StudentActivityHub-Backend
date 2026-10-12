# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Maven files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Fix Windows line endings
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy application source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create upload directory
RUN mkdir -p /app/uploads

# Copy the generated JAR
COPY --from=build /app/target/studentactivityhub-0.0.1-SNAPSHOT.jar app.jar

# Default port
EXPOSE 8082

# Start Spring Boot using Render's assigned port
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8082} -jar app.jar"]