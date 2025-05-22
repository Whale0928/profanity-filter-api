# 빌드 스테이지
FROM eclipse-temurin:21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon --x test

# 실행 스테이지
FROM eclipse-temurin:21
ARG JAR_FILE=/app/profanity-api/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
