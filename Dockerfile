FROM gradle:jdk11 AS builder

ENV GRADLE_SRC_DIR=/home/gradle/src
COPY . $GRADLE_SRC_DIR
WORKDIR $GRADLE_SRC_DIR

RUN gradle jar
RUN gradle jarCloudFunctions

# second stage
FROM amazoncorretto:11

ENV RUN_DIR=/home/

COPY --from=builder /home/gradle/src /app/

WORKDIR /app/

ENTRYPOINT [ "sh", "-c", "java -jar /app/build/libs/profilingCloudFunctions-1.0.jar ${JAVA_PARAMS}" ]
