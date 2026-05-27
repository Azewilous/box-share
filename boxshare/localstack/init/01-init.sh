#!/bin/bash
set -e

echo "--- LocalStack init: creating S3 bucket and SQS queue ---"

awslocal s3 mb s3://box-share-file-store

awslocal sqs create-queue --queue-name box-share-app-queue

QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/box-share-app-queue \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

awslocal s3api put-bucket-notification-configuration \
  --bucket box-share-file-store \
  --notification-configuration "{
    \"QueueConfigurations\": [{
      \"QueueArn\": \"$QUEUE_ARN\",
      \"Events\": [\"s3:ObjectCreated:*\"]
    }]
  }"

echo "--- LocalStack init complete ---"
echo "  Bucket : box-share-file-store"
echo "  Queue  : box-share-app-queue ($QUEUE_ARN)"