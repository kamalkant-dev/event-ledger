# Use lightweight Java image
FROM eclipse-temurin:21-jdk

# Copy jar
COPY target/event-gateway-0.0.1-SNAPSHOT.jar app.jar

# Run app
ENTRYPOINT ["java","-jar","/app.jar"]