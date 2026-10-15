#!/usr/bin/env bash
# Creates the kind cluster "simple-library" and installs the Traefik ingress controller.
# Run once from WSL:  ./k8s/kind/setup.sh
set -euo pipefail
cd "$(dirname "$0")"

for tool in docker kind kubectl helm; do
  command -v "$tool" >/dev/null || { echo "Missing '$tool'. See k8s/README.md, section Prerequisites." >&2; exit 1; }
done
docker info >/dev/null 2>&1 || { echo "Docker is not running or not reachable from this shell." >&2; exit 1; }

if kind get clusters | grep -qx simple-library; then
  echo "kind cluster 'simple-library' already exists, skipping creation."
else
  kind create cluster --config kind-config.yaml
fi
kubectl config use-context kind-simple-library

helm repo add traefik https://traefik.github.io/charts >/dev/null
helm repo update traefik >/dev/null
# Optional: pin a chart version with TRAEFIK_CHART_VERSION=<x.y.z>
helm upgrade --install traefik traefik/traefik \
  --namespace traefik --create-namespace \
  ${TRAEFIK_CHART_VERSION:+--version "$TRAEFIK_CHART_VERSION"} \
  --values traefik-values.yaml --wait --timeout 3m

echo "Cluster and ingress controller are ready. Next: DOCKERHUB_USER=<your-user> ./k8s/kind/deploy.sh"
