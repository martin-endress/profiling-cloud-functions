FROM gcc:9

WORKDIR /usr/src/linpack
COPY linpack/local/ .

RUN gcc -o linpack -O3 -march=native -lm linpack.c

ENTRYPOINT ["./linpack"]
