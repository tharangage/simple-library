#!/usr/bin/env bash
# One-time setup of a public cloud cluster (OKE, GKE, ...): installs cert-manager, the Traefik ingress
# controller (with a cloud load balancer) and the Let's Encrypt issuers. Run from WSL, with kubectl pointing
# at the cloud cluster:
#
#   LETSENCRYPT_EMAIL=you@example.com ./k8s/cloud/setup.sh
#
# Afterwards it prints the public IP of the load balancer: create the DNS records for it (see k8s/README.md),
# then run k8s/cloud/deploy.sh.
set -euo pipefail
cd "$(dirname "$0")"

: "${LETSENCRYPT_EMAIL:?Set LETSENCRYPT_EMAIL, an address for certificate expiry warnings}"
for tool in kubectl helm envsubst; do
  command -v "$tool" >/dev/null || { echo "Missing '$tool'. (envsubst: sudo apt install gettext-base)" >&2; exit 1; }
done

CONTEXT="$(kubectl config current-context)"
case "$CONTEXT" in
  kind-*) echo "The current kubectl context is '$CONTEXT' (a local kind cluster). Switch to the cloud context first:" >&2
          echo "  kubectl config get-contexts && kubectl config use-context <name>" >&2; exit 1 ;;
esac
echo "kubectl context: $CONTEXT"
kubectl get nodes -o wide
read -r -p "Install into this cluster? [y/N] " answer
[ "$answer" = "y" ] || { echo "Cancelled."; exit 1; }

# 1) cert-manager: gets and renews the certificates. crds.enabled installs its custom resources (Issuer, Certificate, ...).
helm repo add jetstack https://charts.jetstack.io >/dev/null
helm repo add traefik https://traefik.github.io/charts >/dev/null
helm repo update jetstack traefik >/dev/null
# Optional: pin chart versions with CERT_MANAGER_CHART_VERSION=<x.y.z> and TRAEFIK_CHART_VERSION=<x.y.z>
helm upgrade --install cert-manager jetstack/cert-manager \
  --namespace cert-manager --create-namespace \
  ${CERT_MANAGER_CHART_VERSION:+--version "$CERT_MANAGER_CHART_VERSION"} \
  --set crds.enabled=true --wait --timeout 5m

# 2) Traefik with a LoadBalancer Service: the cloud creates the load balancer and its public IP.
helm upgrade --install traefik traefik/traefik \
  --namespace traefik --create-namespace \
  ${TRAEFIK_CHART_VERSION:+--version "$TRAEFIK_CHART_VERSION"} \
  --values traefik-values.yaml --wait --timeout 5m

# 3) The Let's Encrypt issuers (only ${LETSENCRYPT_EMAIL} is substituted).
export LETSENCRYPT_EMAIL
envsubst '${LETSENCRYPT_EMAIL}' < cluster-issuers.yaml | kubectl apply -f -

echo
echo "Waiting for the load balancer address (can take a few minutes)..."
for _ in $(seq 1 60); do
  IP="$(kubectl -n traefik get svc traefik -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || true)"
  [ -n "$IP" ] && break
  sleep 5
done
if [ -z "${IP:-}" ]; then
  echo "No address yet. Check: kubectl -n traefik get svc traefik   and   kubectl -n traefik describe svc traefik" >&2
  exit 1
fi
cat <<DONE

Load balancer public IP: $IP

Next:
  1. Create DNS records pointing at $IP (for three names: web, api, auth).
     No domain? With sslip.io the names carry the IP, e.g. BASE_DOMAIN=${IP//./-}.sslip.io needs no records at all.
  2. DOCKERHUB_USER=<your-user> BASE_DOMAIN=<your domain> LETSENCRYPT_EMAIL=$LETSENCRYPT_EMAIL ./k8s/cloud/deploy.sh
DONE
