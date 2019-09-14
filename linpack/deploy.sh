#!/bin/bash

gradle linpack:build

sizes=(128 256 512 768 1024 1280 1536 1792 2048 2304 2560 2816 3008)

for i in "${sizes[@]}"
do
   aws lambda update-function-code --function-name linpack_affinity_$i --zip-file fileb://linpack/build/libs/linpack-1.0.jar
done
