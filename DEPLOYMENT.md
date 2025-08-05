# AWS Elastic Beanstalk Deployment Guide

## Prerequisites

1. **AWS Account Setup**
   - Create an AWS account if you don't have one
   - Create an IAM role for GitHub Actions OIDC authentication
   - Create an S3 bucket for Elastic Beanstalk deployments (or let EB create it)

2. **GitHub Repository Secrets**
   Add these secrets to your GitHub repository:
   - `AWS_DEPLOY_ROLE_ARN`: The ARN of the IAM role for OIDC authentication
   - `AWS_ACCOUNT_ID`: Your AWS account ID

## AWS Setup Instructions

### 1. Create IAM Role for GitHub Actions

Create an IAM role with OIDC trust relationship:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::<YOUR_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:<YOUR_GITHUB_ORG>/<YOUR_REPO_NAME>:*"
        }
      }
    }
  ]
}
```

Attach these policies to the role:
- `AWSElasticBeanstalkWebTier`
- `AWSElasticBeanstalkManagedUpdatesCustomerRolePolicy`
- Custom policy for S3 access:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::elasticbeanstalk-*/*",
        "arn:aws:s3:::elasticbeanstalk-*"
      ]
    }
  ]
}
```

### 2. Create Elastic Beanstalk Application

Using AWS CLI:

```bash
# Create application
aws elasticbeanstalk create-application \
  --application-name langchain4j-ai-agent \
  --description "LangChain4j AI Agent with Claude"

# Create environment
aws elasticbeanstalk create-environment \
  --application-name langchain4j-ai-agent \
  --environment-name langchain4j-ai-agent-env \
  --solution-stack-name "64bit Amazon Linux 2023 v5.0.0 running Corretto 17" \
  --option-settings file://eb-options.json
```

Create `eb-options.json`:

```json
[
  {
    "Namespace": "aws:autoscaling:launchconfiguration",
    "OptionName": "InstanceType",
    "Value": "t3.micro"
  },
  {
    "Namespace": "aws:elasticbeanstalk:environment",
    "OptionName": "EnvironmentType",
    "Value": "SingleInstance"
  }
]
```

### 3. Set Environment Variables

After creating the environment, set the API key:

```bash
aws elasticbeanstalk update-environment \
  --application-name langchain4j-ai-agent \
  --environment-name langchain4j-ai-agent-env \
  --option-settings Namespace=aws:elasticbeanstalk:application:environment,OptionName=ANTHROPIC_API_KEY,Value=YOUR_API_KEY_HERE
```

**Important**: Never commit API keys to your repository!

## Deployment Process

### Automatic Deployment

The GitHub Actions workflow will automatically deploy when you push to the `main` branch:

1. Push code to `main` branch
2. GitHub Actions builds the JAR file
3. Uploads JAR to S3
4. Creates new application version
5. Deploys to Elastic Beanstalk

### Manual Deployment

You can also trigger deployment manually:

1. Go to Actions tab in GitHub
2. Select "Deploy to AWS Elastic Beanstalk"
3. Click "Run workflow"

## Monitoring

### Application Logs

View logs in CloudWatch Logs:
- Log group: `/aws/elasticbeanstalk/langchain4j-ai-agent-env/var/log/eb-app`

### Health Status

Check environment health:

```bash
aws elasticbeanstalk describe-environments \
  --application-name langchain4j-ai-agent \
  --environment-names langchain4j-ai-agent-env
```

## Cost Optimization

This setup uses:
- **t3.micro instance**: Free tier eligible (750 hours/month)
- **Single instance**: No load balancer costs
- **Minimal storage**: Standard S3 rates for JAR storage

Estimated monthly cost (after free tier): ~$10-15

## Troubleshooting

### Common Issues

1. **Deployment fails with "No Application Version"**
   - Check S3 bucket permissions
   - Verify IAM role has correct policies

2. **Application won't start**
   - Check CloudWatch logs
   - Verify ANTHROPIC_API_KEY is set
   - Ensure Java version compatibility

3. **Out of Memory errors**
   - t3.micro has 1GB RAM
   - Adjust JVM settings in `.ebextensions/02-env-vars.config`

### Debug Commands

```bash
# Get environment status
aws elasticbeanstalk describe-environments \
  --application-name langchain4j-ai-agent

# Get recent events
aws elasticbeanstalk describe-events \
  --application-name langchain4j-ai-agent \
  --max-records 20

# SSH into instance (requires key pair setup)
eb ssh langchain4j-ai-agent-env
```

## Security Best Practices

1. **API Key Management**
   - Use AWS Secrets Manager or Parameter Store
   - Rotate keys regularly
   - Never commit keys to repository

2. **Network Security**
   - Configure security groups to limit access
   - Use HTTPS for production
   - Enable AWS WAF if needed

3. **IAM Permissions**
   - Follow least privilege principle
   - Use separate roles for different environments
   - Enable MFA for AWS console access