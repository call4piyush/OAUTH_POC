# CI/CD Setup Guide

This guide will help you set up the complete CI/CD pipeline for the OAuth2 POC project.

## Prerequisites

1. GitHub repository (public or private)
2. Docker registry (Docker Hub, AWS ECR, Azure ACR, etc.)
3. SonarQube server (optional but recommended)
4. Nexus Repository Manager (optional but recommended)
5. Amazon EKS cluster (or any Kubernetes cluster)
6. AWS CLI configured with appropriate permissions

## Step 1: Configure GitHub Secrets

Navigate to your GitHub repository → Settings → Secrets and variables → Actions → New repository secret

Add the following secrets:

### Docker Registry Secrets
```
DOCKER_REGISTRY=your-registry.io
DOCKER_USERNAME=your-username
DOCKER_PASSWORD=your-password
```

### Nexus Secrets (if using Nexus)
```
NEXUS_URL=https://nexus.example.com
NEXUS_USERNAME=admin
NEXUS_PASSWORD=your-password
```

### SonarQube Secrets (if using SonarQube)
```
SONAR_TOKEN=your-sonar-token
SONAR_HOST_URL=https://sonar.example.com
```

### AWS/EKS Secrets
```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
EKS_CLUSTER_NAME=your-cluster-name
AWS_REGION=us-east-1
```

## Step 2: Configure SonarQube (Optional)

1. Install SonarQube server
2. Create a new project in SonarQube
3. Generate an authentication token
4. Add the token to GitHub secrets as `SONAR_TOKEN`
5. Add SonarQube URL to GitHub secrets as `SONAR_HOST_URL`

## Step 3: Configure Nexus (Optional)

1. Install Nexus Repository Manager
2. Create Maven repositories:
   - `maven-releases` (for release artifacts)
   - `maven-snapshots` (for snapshot artifacts)
3. Create a user with deployment permissions
4. Add credentials to GitHub secrets

## Step 4: Set Up EKS Cluster

1. Create an EKS cluster:
   ```bash
   eksctl create cluster --name oauth-poc-cluster --region us-east-1
   ```

2. Configure kubectl:
   ```bash
   aws eks update-kubeconfig --name oauth-poc-cluster --region us-east-1
   ```

3. Verify access:
   ```bash
   kubectl get nodes
   ```

## Step 5: Update Pipeline Configuration

Edit `.github/workflows/ci-cd.yml` and update the following if needed:

- `DOCKER_REGISTRY` - Your container registry URL
- `EKS_CLUSTER_NAME` - Your EKS cluster name
- `AWS_REGION` - Your AWS region

## Step 6: Test the Pipeline

1. Push code to the `main` branch:
   ```bash
   git add .
   git commit -m "Add CI/CD pipeline"
   git push origin main
   ```

2. Monitor the pipeline:
   - Go to GitHub repository → Actions tab
   - Watch the pipeline execution

## Step 7: Verify Deployment

After successful deployment, verify the services:

```bash
# Check pods
kubectl get pods -n oauth-poc

# Check services
kubectl get services -n oauth-poc

# Check deployments
kubectl get deployments -n oauth-poc
```

## Troubleshooting

### Pipeline Fails at Build Stage
- Check Java and Node.js versions in the workflow
- Verify Maven and npm dependencies are correct
- Review build logs for specific errors

### Pipeline Fails at SonarQube
- Verify SonarQube server is accessible
- Check SONAR_TOKEN is valid
- Ensure sonar-project.properties is correct

### Pipeline Fails at Vulnerability Scan
- Review Trivy scan results
- Fix critical vulnerabilities
- Update dependencies

### Pipeline Fails at Docker Build
- Verify Docker registry credentials
- Check Dockerfile syntax
- Ensure build context is correct

### Pipeline Fails at Nexus Upload
- Verify Nexus URL and credentials
- Check Maven distributionManagement configuration
- Ensure Nexus repositories exist

### Pipeline Fails at EKS Deployment
- Verify AWS credentials
- Check EKS cluster access
- Ensure kubectl is configured
- Review Helm chart syntax

## Customization

### Change Pipeline Triggers

Edit `.github/workflows/ci-cd.yml`:

```yaml
on:
  push:
    branches: [ main, develop, release/* ]
  pull_request:
    branches: [ main ]
  workflow_dispatch:  # Allows manual trigger
```

### Add Additional Stages

You can add custom stages to the pipeline:

```yaml
custom-stage:
  name: Custom Stage
  runs-on: ubuntu-latest
  needs: [previous-stage]
  steps:
    - name: Custom Step
      run: echo "Custom action"
```

### Modify Resource Limits

Update Helm chart values in `helm/*/values.yaml`:

```yaml
resources:
  limits:
    cpu: 1000m
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi
```

## Best Practices

1. **Use separate environments**: Create different workflows for dev, staging, and production
2. **Implement approval gates**: Require manual approval for production deployments
3. **Monitor pipeline metrics**: Track build times and success rates
4. **Regular security scans**: Schedule periodic vulnerability scans
5. **Version tagging**: Tag releases with semantic versions
6. **Rollback strategy**: Implement automated rollback on deployment failures
7. **Resource optimization**: Monitor and adjust resource limits based on usage

## Next Steps

1. Set up monitoring and logging (Prometheus, Grafana, ELK)
2. Implement blue-green or canary deployments
3. Add integration tests to the pipeline
4. Set up notification alerts (Slack, email)
5. Configure backup and disaster recovery

## Support

For issues or questions:
1. Check the pipeline logs in GitHub Actions
2. Review the troubleshooting section
3. Consult the Helm and Kubernetes documentation
4. Check service-specific logs in Kubernetes

