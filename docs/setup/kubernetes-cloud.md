# Running Simple Library on a public cloud Kubernetes cluster

The same manifests as on kind (`k8s/base`), with a `cloud` overlay: real host names, https and Let's Encrypt
certificates. The steps are the same on Oracle OKE, GKE and AKS; only the load balancer annotations and the DNS
service differ. For the local setup see [kubernetes.md](kubernetes.md).

## How a request gets from the browser to a pod

1. **Load balancer.** Traefik's Service is of type `LoadBalancer`; the cloud creates a real load balancer with a
   public IP for it (`kubectl -n traefik get svc traefik`, column `EXTERNAL-IP`).
2. **DNS.** An A record per public name (`library.`, `api.`, `auth.` + your domain) points at that IP. The cloud does
   not create the records for you. They live at your registrar or in the cloud's DNS service.
3. **Certificate.** cert-manager asks Let's Encrypt for a certificate. Let's Encrypt proves you own the names by
   fetching a file over plain http (`/.well-known/acme-challenge/...`, the HTTP-01 challenge), which cert-manager
   serves through a temporary Ingress. Port 80 must therefore reach Traefik.
4. **Secret.** The certificate and its key are stored in the Secret `simple-library-tls`. Traefik reads it and
   serves https. cert-manager renews it automatically, about 30 days before it expires (certificates last 90 days).
5. **Ingress.** The `tls:` section of the Ingress and the annotation `cert-manager.io/cluster-issuer` are what
   connect an Ingress to the certificate.

## What is in the repository

| Path | Purpose |
|---|---|
| `k8s/cloud/setup.sh` | Once per cluster: installs cert-manager, Traefik (with the load balancer) and the Let's Encrypt issuers |
| `k8s/cloud/traefik-values.yaml` | Traefik Helm values: LoadBalancer Service (OCI annotations for the free 10 Mbps flexible load balancer), http to https redirect |
| `k8s/cloud/cluster-issuers.yaml` | `letsencrypt-staging` and `letsencrypt-prod` |
| `k8s/cloud/deploy.sh` | Builds `k8s/overlays/cloud` and applies it |
| `k8s/overlays/cloud/` | https host names, Ingress with TLS, Keycloak in production mode (`start`), 50 GB Postgres volume |

`k8s/kind/make-realm.py` is shared with the local setup. `deploy.sh` passes the public addresses to it in the
variables `REALM_WEB_URL`, `REALM_API_URL`, `REALM_AUTH_URL` and `REALM_SSL_REQUIRED`.

## Steps

1. **Cluster and access.** Create the cluster (OKE Basic with A1 nodes, see the plan) and make `kubectl` use it:
   `kubectl config use-context <cloud context>`. Both scripts refuse to run against a `kind-*` context and ask for
   confirmation, so a wrong context cannot deploy here by accident.
2. **Images for ARM.** OKE's free nodes are `arm64`. The GitHub Action publishes `amd64` and `arm64`
   ([docker-hub.md](docker-hub.md)).
3. **Cluster setup (once).**
   ```bash
   LETSENCRYPT_EMAIL=you@example.com ./k8s/cloud/setup.sh
   ```
   It prints the load balancer IP when the cloud has created it.
4. **DNS.** Create three A records for that IP, or use a name that carries the IP, such as
   `<ip with dashes>.sslip.io` (then `BASE_DOMAIN` is that name and no records are needed).
5. **Deploy with staging certificates first.**
   ```bash
   DOCKERHUB_USER=<user> BASE_DOMAIN=<domain> ./k8s/cloud/deploy.sh
   kubectl -n simple-library get certificate        # READY must become True
   ```
   Staging certificates are not trusted by browsers; this step only proves that the challenge works, without using
   up Let's Encrypt rate limits.
6. **Switch to real certificates.**
   ```bash
   CERT_ISSUER=letsencrypt-prod DOCKERHUB_USER=<user> BASE_DOMAIN=<domain> ./k8s/cloud/deploy.sh
   ```
   If the certificate does not change, delete the Secret (`kubectl -n simple-library delete secret simple-library-tls`);
   cert-manager issues a new one.
7. **Identity providers.**
   - Entra ID: add the redirect URI `https://<auth host>/realms/library/broker/entra-id/endpoint`.
   - SAML test provider: the service provider entity ID becomes `https://<auth host>/realms/library`
     (`make-realm.py` already writes it into the imported realm; update the IdP metadata at the provider).
8. **Secrets.** `k8s/overlays/cloud/secrets.env` is generated once and is not committed. It holds the random Keycloak
   admin password. Add `ENTRA_CLIENT_SECRET=<value>` before the first deploy, as for kind.

## Check that it works

```bash
kubectl -n simple-library get pods                       # all Running
kubectl -n simple-library get certificate                # READY True
kubectl -n simple-library describe certificate simple-library-tls
kubectl get challenges -A                                # pending HTTP-01 challenges, if any
curl -I http://<web host>                                # 308 redirect to https
```

## Troubleshooting

| Symptom | Cause |
|---|---|
| `EXTERNAL-IP` stays `<pending>` | The cloud has not created the load balancer: `kubectl -n traefik describe svc traefik` shows the cloud's error (quota, subnet, annotation). |
| Certificate stays not ready, a Challenge is `pending` | DNS does not point at the load balancer yet, or port 80 is blocked (security list / firewall). `kubectl describe challenge -A` gives the reason. |
| Browser warns about the certificate | The staging issuer is in use. Deploy with `CERT_ISSUER=letsencrypt-prod`. |
| Pods `Pending` with memory messages | The nodes are full. Check requests and limits: `kubectl describe node`. |
| `exec format error` in a pod | An `amd64`-only image on an `arm64` node. Check that the image tag lists both platforms on Docker Hub. |

## Notes

- Costs and limits (OKE Basic, Always Free A1, the 10 Mbps load balancer, orphaned load balancers after a delete) are
  in the plan "Simple Library: Cloud and Operators Plan".
- On GKE or AKS delete the OCI annotations in `traefik-values.yaml`. The rest is unchanged.
- Before putting real data on a public cluster, review the realm: the committed file contains localhost redirect URIs
  and test accounts that do not belong on the internet.
