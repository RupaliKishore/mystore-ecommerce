# ============ Stage 1: Build ============
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Maven wrapper + pom.xml copy kar
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Dependencies download kar (cache sathi)
RUN ./mvnw dependency:go-offline -B

# Source code copy kar
COPY src src

# Build kar (skip tests)
RUN ./mvnw clean package -DskipTests

# ============ Stage 2: Run ============
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Build madhun jar copy kar
COPY --from=build /app/target/*.jar app.jar

# Port expose kar
EXPOSE 8085

# App run kar
ENTRYPOINT ["java", "-jar", "app.jar"]