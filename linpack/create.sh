#!/bin/bash

sizes=(128 256 512 768 1024 1280 1536 1792 2048 2304 2560 2816 3008)

gradle linpack:build

for i in "${sizes[@]}"
do
   aws lambda create-function --function-name linpack_affinity_$i --role arn:aws:iam::363072427535:role/aws-lambda-martin --runtime java8 --handler de.uniba.dsg.serverless.calibration.Main::handleRequest --timeout 900 --memory-size $i --zip-file fileb://linpack/build/libs/linpack-1.0.jar
done
