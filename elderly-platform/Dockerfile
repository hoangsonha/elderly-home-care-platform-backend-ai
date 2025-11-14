# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install timezone data for Asia/Ho_Chi_Minh
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && \
    echo "Asia/Ho_Chi_Minh" > /etc/timezone

# Copy built jar from builder stage
COPY --from=builder /app/target/elderly-platform-0.0.1-SNAPSHOT.jar app.jar

# Create directory for Firebase keys
RUN mkdir -p /app/keys

# Set timezone environment variable
ENV TZ=Asia/Ho_Chi_Minh

# Expose port
EXPOSE 8080

# Run the application with timezone setting
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
