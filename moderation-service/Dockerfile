# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY ../pom.xml ./pom.xml
COPY ../fanzone-common ./fanzone-common
COPY moderation-service ./moderation-service
RUN apt-get update && apt-get install -y maven
RUN mvn -f pom.xml -pl moderation-service -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/moderation-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
