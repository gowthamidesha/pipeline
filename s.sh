#!/bin/bash

# Set environment variables
REGION="us-west-2" # Update with your desired AWS region
LAMBDA_FUNCTION_NAME="myLambdaFunction"

# Create CloudWatch Events rule to trigger Lambda function
aws events put-rule \
    --name 'LambdaTriggerRule' \
    --schedule-expression 'cron(0 9 ? * MON *)' \
    --region ${REGION}

# Add permission for CloudWatch Events to invoke Lambda
aws lambda add-permission \
    --function-name ${LAMBDA_FUNCTION_NAME} \
    --principal events.amazonaws.com \
    --statement-id 'LambdaInvokePermission' \
    --action 'lambda:InvokeFunction' \
    --source-arn arn:aws:events:${REGION}:$(aws sts get-caller-identity --query Account --output text):rule/LambdaTriggerRule \
    --region ${REGION}

# Attach Lambda function to CloudWatch Events rule
aws events put-targets \
    --rule 'LambdaTriggerRule' \
    --targets Id=1,Arn=$(aws lambda get-function --function-name ${LAMBDA_FUNCTION_NAME} --query 'Configuration.FunctionArn' --output text) \
    --region ${REGION}
