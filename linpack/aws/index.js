'use strict';
const AWS = require('aws-sdk');
const exec = require('child_process').exec;

// If parameter experiment is set, the profile is saved under that name.
// Else, the current time is used as a name.
exports.handler = (event, context) => {
  const s3 = new AWS.S3({ signatureVersion: 'v4' });

  var name = getToday()
  if (event.queryStringParameters != null) {
    name = event.queryStringParameters.experiment || name;
  }
  const filePath = "linpack/" + context.memoryLimitInMB + "/" + name;
  const start = Date.now();

  exec("./runme64", (error, stdout, stderr) => {
    const end = Date.now();
    const diff = end - start;

    const rows = stdout.split("\n")
    const row = rows[rows.length - 8];
    const flops = parseFloat(row.split(/[\s,]+/)[4]);

    const output = flops + " " + start + " " + end + " " + diff;

    s3.putObject({
      Bucket: 'martin-linpack',
      Key: filePath,
      Body: stdout
    }, callback(output));
  });

  function callback(path) {
    return (err, resp) => {
      if (err) {
        console.log(err);
        context.error(err);
      } else {
        console.log('Successfully uploaded package to ' + path);
        var response = {
          "statusCode": 200,
          "body": filePath,
          "isBase64Encoded": false
        };
        context.succeed(response);
      }
    }
  }

  function getToday() {
    var today = new Date();
    var yyyy = today.getFullYear();
    var MM = String(today.getMonth() + 1).padStart(2, '0');
    var DD = String(today.getDate()).padStart(2, '0');
    var HH = String(today.getHours()).padStart(2, '0');
    var mm = String(today.getMinutes()).padStart(2, '0');
    return yyyy + '_' + MM + '_' + DD + '-' + HH + '_' + mm;
  }

};

