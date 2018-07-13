FROM openjdk:8-jdk-alpine AS builder
WORKDIR /src
ADD . /src
RUN ./gradlew clean assemble

FROM openjdk:8-jre-alpine
RUN apk --no-cache add docker git
WORKDIR /app
COPY --from=builder /src/build/libs/*-0.0.1-SNAPSHOT.jar .
CMD exec java -jar *.jar
