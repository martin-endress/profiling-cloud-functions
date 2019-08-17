FROM gradle:jdk11 AS builder
WORKDIR /home/gradle/
COPY . .
RUN gradle :executor:build

FROM amazoncorretto:11
COPY --from=builder /home/gradle/executor/build/libs/* /app/
WORKDIR /app/
CMD [ "sh", "-c", "java -jar executor-1.0.jar ${JAVA_PARAMS}" ]