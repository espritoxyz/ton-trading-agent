# syntax=docker/dockerfile:1.7
FROM gradle:8.9-jdk21 AS build
WORKDIR /app

ARG GITHUB_USERNAME
ARG GITHUB_PAT
ARG OPENAI_API_KEY
ENV GITHUB_USERNAME=${GITHUB_USERNAME}
ENV GITHUB_PAT=${GITHUB_PAT}
ENV OPENAI_API_KEY=${OPENAI_API_KEY}

COPY . /app

RUN --mount=type=cache,target=/home/gradle/.gradle \
    --mount=type=cache,target=/app/.gradle \
    gradle --no-daemon :agent-backend:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/agent-backend/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
