FROM gradle:jdk11 AS builder

ENV GRADLE_SRC_DIR=/home/gradle/src
COPY . $GRADLE_SRC_DIR
WORKDIR $GRADLE_SRC_DIR

RUN gradle jar

# second stage
FROM amazoncorretto:11

ENV RUN_DIR=/home/

COPY --from=builder /home/gradle/src /app/

WORKDIR /app/

ENTRYPOINT [ "java", "-jar", "/app/build/libs/profilingCloudFunctions-1.0.jar" ]
