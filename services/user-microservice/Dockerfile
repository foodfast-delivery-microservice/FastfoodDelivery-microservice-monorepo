# ----- Giai đoạn 1: Build -----
# Dùng image có Maven và JDK 22
FROM maven:3.9.8-eclipse-temurin-22-jammy AS build

WORKDIR /src
COPY pom.xml .
COPY src ./src

# Chạy lệnh build
RUN mvn clean package -DskipTests

# ----- Giai đoạn 2: Run -----
# Dùng image JRE 22 (alpine) để chạy app
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

# Copy file .jar đã build từ Giai đoạn 1
COPY --from=build /src/target/*.jar app.jar

EXPOSE 8081

# Lệnh chạy app
ENTRYPOINT ["java","-jar","/app/app.jar"]