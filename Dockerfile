# Use a multi-stage build for smaller image
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# Files checked out on Windows may have CRLF line endings, which break the wrapper script in Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
