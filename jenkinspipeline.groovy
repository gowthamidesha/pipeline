pipeline {
    agent any

    environment {
        // Define environment variables
        CF_STACK_NAME = 'my-lambda-stack'
        CF_TEMPLATE_FILE = 'lambda-cloudformation-template.yaml'
        LAMBDA_PACKAGE_FILE = 'lambda-package.zip'
        LAMBDA_FUNCTION_NAME = 'myLambdaFunction'
        REGION = 'us-west-2' // Update with your desired AWS region
    }

    stages {
        stage('Build') {
            steps {
                script {
                    // Package your Lambda function here if needed
                    // Example: sh 'zip -r ${LAMBDA_PACKAGE_FILE} lambda-function/'
                }
            }
        }

        stage('Deploy CloudFormation Stack') {
            steps {
                script {
                    // Deploy CloudFormation stack to AWS
                    sh """
                    aws cloudformation deploy \
                        --template-file ${CF_TEMPLATE_FILE} \
                        --stack-name ${CF_STACK_NAME} \
                        --region ${REGION} \
                        --capabilities CAPABILITY_NAMED_IAM \
                        --parameter-overrides LambdaPackage=${LAMBDA_PACKAGE_FILE}
                    """
                }
            }
        }

        stage('Create CloudWatch Event Rule') {
            steps {
                script {
                    // Create CloudWatch Events rule to trigger Lambda function
                    sh """
                    aws events put-rule \
                        --name 'LambdaTriggerRule' \
                        --schedule-expression 'cron(0 9 ? * MON *)' \
                        --region ${REGION}
                    """

                    // Add permission for CloudWatch Events to invoke Lambda
                    sh """
                    aws lambda add-permission \
                        --function-name ${LAMBDA_FUNCTION_NAME} \
                        --principal events.amazonaws.com \
                        --statement-id 'LambdaInvokePermission' \
                        --action 'lambda:InvokeFunction' \
                        --source-arn arn:aws:events:${REGION}:$(aws sts get-caller-identity --query Account --output text):rule/LambdaTriggerRule \
                        --region ${REGION}
                    """

                    // Attach Lambda function to CloudWatch Events rule
                    sh """
                    aws events put-targets \
                        --rule 'LambdaTriggerRule' \
                        --targets Id=1,Arn=$(aws lambda get-function --function-name ${LAMBDA_FUNCTION_NAME} --query 'Configuration.FunctionArn' --output text) \
                        --region ${REGION}
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
