FROM amazoncorretto:11

WORKDIR "/home"

COPY * .

ENTRYPOINT ./gradlew