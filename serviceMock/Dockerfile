FROM gradle:jdk11 AS builder

WORKDIR /home/gradle/
COPY . .

RUN gradle :serviceMock:build

FROM amazoncorretto:11

COPY --from=builder /home/gradle/serviceMock/build/libs/* /app/

WORKDIR /app/

EXPOSE 9000

CMD [ "sh", "-c", "java -jar serviceMock-1.0.jar" ]
