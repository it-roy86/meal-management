# ---- 1단계: 빌드 ----
# Maven으로 JAR 파일을 빌드해요.
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# ---- 2단계: 실행 ----
# 빌드된 JAR 파일만 가져와서 실행해요.
# 이미지 크기를 줄이기 위해 JRE만 포함된 이미지를 사용해요.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]