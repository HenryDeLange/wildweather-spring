# WildWeather Server

![App Version](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2FHenryDeLange%2Fwildweather-spring%2Fmain%2Fpom.xml&query=%2F*%5Blocal-name()%3D'project'%5D%2F*%5Blocal-name()%3D'version'%5D&label=version)
![GitHub License](https://img.shields.io/github/license/HenryDeLange/wildweather-spring)

The WildWeather Server is responsible for processing Ambient Weather files containing historic weather records form my personal weather stations.

😶‍🌫️ [Live API](https://server.mywild.co.za/wildweather-api) 😶‍🌫️

See the [wildweather-react](https://github.com/HenryDeLange/wildweather-react) project for the web UI.

## Development

![Build](https://img.shields.io/github/actions/workflow/status/HenryDeLange/wildweather-spring/spring-source-build.yml?label=build)

This project is a standard _Java_ _Spring Boot_ application that uses _Maven_ as the build tool.

[OpenAPI Specification](./src/main/openapi/api.yml)

[https://localhost:8080](https://localhost:8080)

### Requirements

- [Java JDK 25 Lite](https://bell-sw.com/pages/downloads/)
- [Maven 3.9.9](https://maven.apache.org/)
- [Encryption Keys](#encryption-keys)

### Maven Commands

Action | Command
-|-
Build | `./mvnw clean verify`
Run | `./mvnw spring-boot:run`
Code Analyses | `./mvnw clean verify -P report` and then `./mvnw site -P report`. View the results on the [Maven Site](./target/site/index.html).

### Docker Commands

Action | Command
-|-
Run | `docker compose up -d`
Run (rebuild Docker image) | `docker compose up -d --build`
Stop | `docker compose down`

## Release

![Release](https://img.shields.io/github/actions/workflow/status/HenryDeLange/wildweather-spring/spring-release-build.yml?label=release)

### Publish To GitHub Releases and Packages

Run the [spring-release-build.yml](./.github/workflows/spring-release-build.yml) _Github Action_.

### Manual Maven Build

Use the below command to build a production ready artifact.

`./mvnw clean verify -P prod`

The production artifact will be the [wildweather-app.jar](./target/wildweather-app.jar) file.

#### Dependency Licenses

See [licenses.xml](target/generated-resources/licenses.xml) for license details.

#### Code Report

A code report for `SpotBugs`, `PMD` and `CPD` can be generated using the below commands:\
`./mvnw clean verify -P report`\
`./mvnw site -P report`

To view the results of the code analysis open the [Maven Site](./target/site/index.html) webpage.

## Configuration

### Application Properties

See the _Spring Boot_ [application.yml](./src/main/resources/application.yml) configuration file.

### Environment Variables

See the [.env.production](./.env.production) file.

Note that the other _.env_ files are used during development.

### Localisation

See the [messages_en.properties](./src/main/resources/messages_en.properties) and related files.

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

Then copy the [private_key_pkcs8.pem](./keys/jwt/private_key_pkcs8.pem) and [public_key.pem](./keys/jwt/public_key.pem) values into the relevant `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` environment variables respectively in the [.env.local](./.env.local) file for development, or set it in the production container's configuration.
