# -------- STAGE 1: Build --------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn -B -q -e -DskipTests dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# -------- STAGE 2: Run --------
FROM eclipse-temurin:17-jdk

# FFmpeg install
RUN apt-get update && apt-get install -y ffmpeg

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
