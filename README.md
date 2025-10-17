# wildweather-spring

The WildWeather server is responsible for processing the Ambient Weather files containing historic weather conditions for my personal weather stations.

## Development

This project is a standard _Java_ _Spring Boot_ application that uses _Maven_ as the build tool.

[OpenAPI Specification](./src/main/openapi/api.yml)

[https://localhost:8080](https://localhost:8080)

### Requirements

- [Java JDK 25 Lite](https://bell-sw.com/pages/downloads/)
- [Maven 3.9.9](https://maven.apache.org/)

### Maven Commands

Action | Command
-|-
Build | `./mvnw clean verify`
Run | `./mvnw spring-boot:run`
Code Analyses | `./mvnw clean verify -P report` and then `./mvnw site -P report`. View results on the [Maven Site](./target/site/index.html) webpage.

### Docker Commands

Action | Command
-|-
Run | `docker compose up -d`
Run (rebuild Docker image) | `docker compose up -d --build`
Stop | `docker compose down`

## Release

### Maven Build

Use the below command to build a production ready artifact.

`./mvnw clean verify -P prod`

The production artifact will be the [wildweather-app.jar](./target/wildweather-app.jar) file.

#### Dependency Licenses

See [licenses.xml](target/generated-resources/licenses.xml) for license details.

## Encryption Keys

_Note: This project comes with already generated development keys, however any production deployment should use newly generated keys._

Use `openssl` to create the necessary keys.\
(In Windows you can use [WSL](https://learn.microsoft.com/en-us/windows/wsl/install) to execute the commands.)

From the project root folder, run the commands in the sections below in order to generate the new keys.

### JWT Keys

```sh
mkdir -p ./keys/jwt

openssl genrsa -out ./keys/jwt/private_key.pem 2048

openssl rsa -in ./keys/jwt/private_key.pem -outform PEM -pubout -out ./keys/jwt/public_key.pem

openssl pkcs8 -topk8 -inform PEM -in ./keys/jwt/private_key.pem -outform PEM -nocrypt -out ./keys/jwt/private_key_pkcs8.pem
```

Then copy the [private_key_pkcs8.pem](./keys/jwt/private_key_pkcs8.pem) and [public_key.pem](./keys/jwt/public_key.pem) values into the relevant `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` environment variables respectively in the [.env.local](./.env.local) file for development, or set it in the production configuration.

