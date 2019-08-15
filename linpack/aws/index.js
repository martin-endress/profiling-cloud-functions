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

  exec("./linpack", (error, stdout, stderr) => {
    s3.putObject({
      Bucket: 'martin-linpack',
      Key: filePath,
      Body: stdout
    }, callback(name));
  });
  var response = {
    "statusCode": 200,
    "body": filePath,
    "isBase64Encoded": false
  };

  context.succeed(response);

  function callback(path) {
    return (err, resp) => {
      if (err) {
        console.log(err);
      } else {
        console.log('Successfully uploaded package to ' + path);
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

