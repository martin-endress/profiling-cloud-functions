#!/bin/bash

zip linpack.zip *
echo "linpack.zip created"

sizes=(128 256 512 1024 2048 3008)
for i in "${sizes[@]}"
do
   aws lambda update-function-code --function-name linpack_$i --zip-file  fileb://linpack.zip
done
