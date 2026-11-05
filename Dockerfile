# Stage 1: Build
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copiar archivos del wrapper y dependencias
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Copiar código fuente y empaquetar omitiendo pruebas
COPY src src
RUN ./mvnw -B clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copiar el archivo jar generado
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
