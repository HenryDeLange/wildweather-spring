# Build the application
FROM bellsoft/liberica-runtime-container:jdk-25-musl AS builder
WORKDIR /app
ADD ./ /app/
RUN chmod +x ./mvnw && ./mvnw clean package -P prod

# Run the application
FROM bellsoft/liberica-runtime-container:jre-25-slim-musl AS production
WORKDIR /app
EXPOSE 8080
CMD ["java", "-jar", "/app/wildweather-app.jar"]
COPY --from=builder /app/target/wildweather-app.jar /app/wildweather-app.jar
