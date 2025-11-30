# Helm Charts

This directory contains Helm charts for deploying the OAuth2 POC application to Kubernetes.

## Chart Structure

- `oauth-poc/` - Parent chart that orchestrates all services
- `api-gateway/` - API Gateway service chart
- `user-service/` - User Service chart
- `bff/` - Backend for Frontend service chart
- `auth-ui/` - Authentication UI frontend chart

## Installation

### Install All Services
```bash
helm install oauth-poc ./helm/oauth-poc \
  --namespace oauth-poc \
  --create-namespace \
  --set imageRegistry=your-registry.io \
  --set api-gateway.image.tag=v1.0.0 \
  --set user-service.image.tag=v1.0.0 \
  --set bff.image.tag=v1.0.0 \
  --set auth-ui.image.tag=v1.0.0
```

### Install Individual Service
```bash
helm install api-gateway ./helm/api-gateway \
  --namespace oauth-poc \
  --set image.registry=your-registry.io \
  --set image.tag=v1.0.0
```

## Upgrading

```bash
helm upgrade oauth-poc ./helm/oauth-poc \
  --namespace oauth-poc \
  --set imageRegistry=your-registry.io \
  --set api-gateway.image.tag=v1.0.1
```

## Uninstallation

```bash
helm uninstall oauth-poc --namespace oauth-poc
```

## Configuration

### Environment Variables

Each service can be configured through values.yaml or via command-line flags:

```bash
helm install oauth-poc ./helm/oauth-poc \
  --set api-gateway.env.KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/poc \
  --set user-service.env.SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/users
```

### Resource Limits

Adjust resource limits and requests:

```bash
helm install oauth-poc ./helm/oauth-poc \
  --set api-gateway.resources.limits.cpu=1000m \
  --set api-gateway.resources.limits.memory=1Gi
```

### Replica Count

Scale services:

```bash
helm upgrade oauth-poc ./helm/oauth-poc \
  --set api-gateway.replicaCount=3 \
  --set user-service.replicaCount=3
```

### Horizontal Pod Autoscaling

Enable autoscaling:

```bash
helm upgrade oauth-poc ./helm/oauth-poc \
  --set api-gateway.autoscaling.enabled=true \
  --set api-gateway.autoscaling.minReplicas=2 \
  --set api-gateway.autoscaling.maxReplicas=10
```

## Dependencies

The parent chart (`oauth-poc`) includes dependencies on all service charts. To update dependencies:

```bash
cd helm/oauth-poc
helm dependency update
```

## Chart Values

Each chart has a `values.yaml` file with default configurations. Key values include:

- `replicaCount` - Number of pod replicas
- `image.registry` - Container image registry
- `image.repository` - Container image repository
- `image.tag` - Container image tag
- `service.type` - Kubernetes service type
- `resources` - CPU and memory limits/requests
- `env` - Environment variables
- `autoscaling` - Horizontal Pod Autoscaler configuration

## Health Checks

All services include liveness and readiness probes:

- **Liveness Probe**: Checks if the container is running
- **Readiness Probe**: Checks if the container is ready to serve traffic

Probe paths:
- Spring Boot services: `/actuator/health/liveness` and `/actuator/health/readiness`
- BFF service: `/health`
- Auth UI: `/`

## Ingress

Ingress is configured for the auth-ui service by default. To enable ingress for other services, update their `values.yaml` files.

## Security

- Service accounts are created for each service
- Security contexts are configured
- Network policies can be added for additional security

## Monitoring

All services expose metrics endpoints:
- Spring Boot services: `/actuator/metrics`
- Health endpoints: `/actuator/health`

## Troubleshooting

### Check Pod Status
```bash
kubectl get pods -n oauth-poc
```

### View Logs
```bash
kubectl logs -n oauth-poc deployment/api-gateway
```

### Describe Pod
```bash
kubectl describe pod -n oauth-poc <pod-name>
```

### Test Service
```bash
kubectl port-forward -n oauth-poc svc/api-gateway 8080:8080
curl http://localhost:8080/actuator/health
```

