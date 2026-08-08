# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY ../pom.xml ./pom.xml
COPY ../fanzone-common ./fanzone-common
COPY feed-service ./feed-service
RUN apt-get update && apt-get install -y maven
RUN mvn -f pom.xml -pl feed-service -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/feed-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
