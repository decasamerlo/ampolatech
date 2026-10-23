# syntax=docker/dockerfile:1

# The newest Temurin build Docker Hub publishes, which can trail the one
# mise.toml pins.
FROM eclipse-temurin:25.0.4.1_1-jdk AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:25.0.4.1_1-jre
RUN groupadd --system ampolatech && useradd --system --gid ampolatech ampolatech
COPY --from=build /workspace/app/build/libs/ampolatech.jar /opt/ampolatech/ampolatech.jar
USER ampolatech
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/ampolatech/ampolatech.jar"]
