FROM gradle:jdk11 AS builder

ENV GRADLE_SRC_DIR=/home/gradle/src
COPY . $GRADLE_SRC_DIR
WORKDIR $GRADLE_SRC_DIR

RUN gradle build

FROM amazoncorretto:11

COPY --from=builder /home/gradle/src/build/libs/* /app/

WORKDIR /app/

ENTRYPOINT [ "sh", "-c", "java -jar /app/profilingCloudFunctions-1.0.jar ${JAVA_PARAMS}" ]
