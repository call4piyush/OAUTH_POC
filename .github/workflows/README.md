# CI/CD Pipeline Documentation

This repository includes a comprehensive CI/CD pipeline that automates the build, test, security scanning, and deployment process.

## Pipeline Stages

### 1. Build and Test
- Builds all backend services (Maven)
- Builds frontend applications (npm)
- Runs unit tests for all services
- Generates test coverage reports

### 2. SonarQube Analysis
- Performs static code analysis
- Checks code quality metrics
- Enforces quality gates
- Generates code coverage reports

### 3. Vulnerability Scan
- Scans Java dependencies using Trivy
- Scans Node.js dependencies using Trivy
- Scans Docker images for vulnerabilities
- Uploads results to GitHub Security

### 4. Build and Push Docker Images
- Builds Docker images for all services
- Pushes images to container registry
- Tags images with commit SHA and latest

### 5. Upload to Nexus
- Uploads Maven artifacts to Nexus repository
- Supports both releases and snapshots

### 6. Package Helm Charts
- Packages all Helm charts
- Creates versioned chart packages
- Prepares charts for deployment

### 7. Deploy to EKS
- Deploys application to Amazon EKS cluster
- Uses Helm for deployment
- Verifies deployment status

## Required Secrets

Configure the following secrets in your GitHub repository:

### Docker Registry
- `DOCKER_REGISTRY` - Docker registry URL (e.g., `your-registry.io`)
- `DOCKER_USERNAME` - Docker registry username
- `DOCKER_PASSWORD` - Docker registry password

### Nexus
- `NEXUS_URL` - Nexus repository URL
- `NEXUS_USERNAME` - Nexus username
- `NEXUS_PASSWORD` - Nexus password

### SonarQube
- `SONAR_TOKEN` - SonarQube authentication token
- `SONAR_HOST_URL` - SonarQube server URL

### AWS/EKS
- `AWS_ACCESS_KEY_ID` - AWS access key
- `AWS_SECRET_ACCESS_KEY` - AWS secret key
- `EKS_CLUSTER_NAME` - EKS cluster name
- `AWS_REGION` - AWS region (default: us-east-1)

## Pipeline Triggers

The pipeline runs on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches

## Manual Configuration

### SonarQube Setup
1. Install and configure SonarQube server
2. Create a project in SonarQube
3. Generate an authentication token
4. Add token to GitHub secrets

### Nexus Setup
1. Install and configure Nexus Repository Manager
2. Create Maven repositories (releases and snapshots)
3. Configure user credentials
4. Add credentials to GitHub secrets

### EKS Setup
1. Create an EKS cluster
2. Configure kubectl access
3. Install Helm in the cluster
4. Add AWS credentials to GitHub secrets

## Local Testing

### Run Tests Locally
```bash
cd backend
mvn clean test
```

### Build Docker Images Locally
```bash
docker build -t api-gateway:local ./backend/api-gateway
docker build -t user-service:local ./backend/user-service
docker build -t bff:local ./services/bff
docker build -t auth-ui:local ./frontend/auth-ui
```

### Test Helm Charts Locally
```bash
helm lint helm/api-gateway
helm lint helm/user-service
helm lint helm/bff
helm lint helm/auth-ui
helm lint helm/oauth-poc
```

### Dry Run Helm Deployment
```bash
helm install oauth-poc helm/oauth-poc --dry-run --debug
```

## Troubleshooting

### Pipeline Fails at SonarQube
- Verify SonarQube server is accessible
- Check SONAR_TOKEN is valid
- Ensure sonar-project.properties is configured correctly

### Pipeline Fails at Vulnerability Scan
- Review Trivy scan results
- Fix critical and high severity vulnerabilities
- Update dependencies if needed

### Deployment Fails
- Verify EKS cluster is accessible
- Check AWS credentials are correct
- Ensure kubectl is configured properly
- Verify Helm charts are valid

## Best Practices

1. **Always review vulnerability scan results** before deploying
2. **Monitor SonarQube quality gates** to maintain code quality
3. **Use semantic versioning** for releases
4. **Tag Docker images** with meaningful versions
5. **Test Helm charts** locally before pushing
6. **Monitor deployment status** after each release

