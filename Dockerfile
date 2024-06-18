FROM eclipse-temurin:17-jdk
ARG JAR_FILE=module-api/build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
