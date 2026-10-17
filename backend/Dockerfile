# Use a multi-stage build for smaller image.
# The build stage always runs on the CI machine's own CPU ($BUILDPLATFORM): the jar is the same for every CPU,
# so it is built once and not under emulation.
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
# Files checked out on Windows may have CRLF line endings, which break the wrapper script in Linux
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime image. Not the -alpine tag: eclipse-temurin:17-jre-alpine exists for amd64 only, and the image must also run
# on ARM (Oracle Cloud A1 nodes, Apple silicon).
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
