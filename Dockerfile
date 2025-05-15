# 빌드 스테이지
FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/profanity-api/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
