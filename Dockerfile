# 빌드 스테이지
FROM gradle:7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app
# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/profanity-api/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
