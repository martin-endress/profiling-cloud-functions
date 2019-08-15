#!/bin/bash

sizes=(128 256 512 1024 2048 3008)
for i in "${sizes[@]}"
do
   aws lambda create-function --function-name linpack_$i --runtime nodejs8.10 --role arn:aws:iam::327417652975:role/Martin --handler index.handler --timeout 900 --memory-size $i --zip-file fileb://linpack.zip
done
