FROM gradle:jdk11 AS BUILDER

WORKDIR /home/gradle
COPY . .
RUN gradle jar

FROM adoptopenjdk/openjdk11:alpine-jre

COPY --from=BUILDER /home/gradle/build/libs/images-service.jar /app/images-service.jar

CMD ["java", "-jar", "/app/images-service.jar"]
