# ---- 빌드 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ---- 실행 ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 wbw
COPY --from=build /src/build/libs/*.jar /app/app.jar
COPY fonts /app/fonts
ENV PDF_FONT_DIR=/app/fonts \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
USER wbw
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
