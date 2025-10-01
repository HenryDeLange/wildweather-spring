# Build a normal image

FROM bellsoft/liberica-runtime-container:jdk-24-musl AS builder
WORKDIR /app
ADD ./ /app/
RUN chmod +x ./mvnw && ./mvnw clean package -P prod

FROM bellsoft/liberica-runtime-container:jre-24-slim-musl
WORKDIR /app
EXPOSE 8080
CMD ["java", "-jar", "/app/wildweather-app.jar"]
COPY --from=builder /app/target/wildweather-app.jar /app/wildweather-app.jar
