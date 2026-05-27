# Build stage
FROM maven:3.8-eclipse-temurin-17 AS build
COPY src /home/app/src

COPY pom.xml /home/app
# Build and list the target directory to verify JAR creation
RUN mvn -f /home/app/pom.xml clean package -DskipTests=true && ls -la /home/app/target/

# Package stage
FROM eclipse-temurin:17-jre-focal
COPY --from=build /home/app/target/bds-0.0.1-SNAPSHOT.jar /usr/local/lib/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/usr/local/lib/app.jar"]