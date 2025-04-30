FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon
EXPOSE 8080
CMD ["java", "-jar", "build/libs/oneup-0.0.1-SNAPSHOT.jar"]
