#!/usr/bin/env python3
"""Converts the Keycloak realm file between its committed form and the form the local cluster needs.

  make-realm.py <realm-library.json> <output.json>
      Committed file -> cluster (used by deploy.sh):
        - adds the cluster URLs (library.localhost, api.localhost) to the redirect URIs and web
          origins of the clients library-web and library-swagger; the localhost entries stay
        - replaces the masked secret of library-backend ("**********") with ${LIBRARY_BACKEND_CLIENT_SECRET},
          and the masked client secret of the identity provider entra-id with ${ENTRA_CLIENT_SECRET};
          Keycloak fills both from environment variables when it imports the realm
        - changes the host of the SAML service provider entity ID of the saml identity providers to the
          cluster address (http://localhost), keeping the path: the entity ID must match the address the
          IdP sees. Whatever host the committed file has (e.g. http://localhost:8080) is replaced.
        - sets sslRequired to "none" (local cluster, plain http)

  make-realm.py --export [--previous <committed-realm.json>] <raw-export.json> <realm-library.json>
      Export from the running cluster -> committed file (used by export-realm.sh):
        - removes the cluster URLs again, so the committed file stays free of local-only values
        - puts the host of the SAML service provider entity ID back to the one in --previous (the committed
          file being replaced), per identity provider alias; the path comes from the export. Without
          --previous, or for an alias not in it, the exported value is kept.
        - sets sslRequired back to "external" (the Keycloak default)
        - writes the library-backend secret and the entra-id client secret as the masked value "**********",
          as in a console export
"""
import json
import os
import sys
from urllib.parse import urlsplit, urlunsplit

WEB = "http://library.localhost"
API = "http://api.localhost"
ADD = {
    "library-web": {"redirectUris": [WEB + "/*", WEB], "webOrigins": [WEB]},
    "library-swagger": {"redirectUris": [API + "/swagger-ui/*"], "webOrigins": [API]},
}
PLACEHOLDER = "${LIBRARY_BACKEND_CLIENT_SECRET}"
MASKED = "**********"
# Identity providers whose client secret is injected from an environment variable: alias -> placeholder.
# An alias that is not in the realm file is simply skipped.
IDP_SECRETS = {"entra-id": "${ENTRA_CLIENT_SECRET}"}
# Address Keycloak is served at in the cluster (must match KC_HOSTNAME in overlays/local/env-local.yaml).
CLUSTER_ORIGIN = "http://localhost"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save(path, realm):
    with open(path, "w", encoding="utf-8") as f:
        # No trailing newline, like the file Keycloak writes, so re-exports show only real changes in git.
        json.dump(realm, f, indent=2, ensure_ascii=False)


def extend(target, key, values):
    current = target.setdefault(key, [])
    for v in values:
        if v not in current:
            current.append(v)


def logout_uris(client, add, values):
    """'Valid post logout redirect URIs' is a '##'-separated string; '+' already means "same as redirect URIs"."""
    attrs = client.get("attributes", {})
    current = attrs.get("post.logout.redirect.uris")
    if not current or current == "+":
        return
    parts = current.split("##")
    parts = [p for p in parts if p not in values] if not add else parts + [v for v in values if v not in parts]
    if parts:
        attrs["post.logout.redirect.uris"] = "##".join(parts)
    else:
        del attrs["post.logout.redirect.uris"]


def idp_secrets(realm, to_placeholder):
    for idp in realm.get("identityProviders", []):
        placeholder = IDP_SECRETS.get(idp.get("alias"))
        config = idp.get("config")
        if not placeholder or config is None:
            continue
        if to_placeholder:
            config["clientSecret"] = placeholder
        elif config.get("clientSecret") in (None, placeholder):
            config["clientSecret"] = MASKED


def with_origin(url, origin):
    """url with its scheme and host (and port) replaced by those of origin; path and query are kept."""
    u, o = urlsplit(url), urlsplit(origin)
    return urlunsplit((o.scheme, o.netloc, u.path, u.query, u.fragment))


def sp_entity_ids(realm):
    """The (idp config) of saml providers whose entity ID is Keycloak's own, i.e. <host>/realms/<realm>."""
    for idp in realm.get("identityProviders", []):
        config = idp.get("config") or {}
        if idp.get("providerId") == "saml" and urlsplit(config.get("entityId", "")).path.startswith("/realms/"):
            yield idp["alias"], config


def saml_entity_id_to_cluster(realm):
    for _, config in sp_entity_ids(realm):
        config["entityId"] = with_origin(config["entityId"], CLUSTER_ORIGIN)


def saml_entity_id_from_cluster(realm, previous):
    before = dict(sp_entity_ids(previous)) if previous else {}
    for alias, config in sp_entity_ids(realm):
        if alias in before:
            config["entityId"] = with_origin(config["entityId"], before[alias]["entityId"])


def to_cluster(realm):
    seen = set()
    for client in realm.get("clients", []):
        cid = client["clientId"]
        if cid in ADD:
            seen.add(cid)
            for key, values in ADD[cid].items():
                extend(client, key, values)
            logout_uris(client, True, ADD[cid]["redirectUris"])
        elif cid == "library-backend":
            seen.add(cid)
            client["secret"] = PLACEHOLDER
    missing = {"library-web", "library-swagger", "library-backend"} - seen
    if missing:
        sys.exit("Client(s) not found in the realm file: " + ", ".join(sorted(missing)))
    idp_secrets(realm, True)
    saml_entity_id_to_cluster(realm)
    realm["sslRequired"] = "none"
    return realm


def from_cluster(realm, previous=None):
    if realm.get("realm") != "library":
        sys.exit("The export is not the realm 'library' (found: %r)" % realm.get("realm"))
    for client in realm.get("clients", []):
        cid = client["clientId"]
        if cid in ADD:
            for key, values in ADD[cid].items():
                if key in client:
                    client[key] = [v for v in client[key] if v not in values]
            logout_uris(client, False, ADD[cid]["redirectUris"])
        elif cid == "library-backend" and client.get("secret") in (None, PLACEHOLDER):
            client["secret"] = MASKED
    idp_secrets(realm, False)
    saml_entity_id_from_cluster(realm, previous)
    realm["sslRequired"] = "external"
    return realm


def main(argv):
    export = "--export" in argv
    previous = None
    args = [a for a in argv if a != "--export"]
    if "--previous" in args:
        i = args.index("--previous")
        if i + 1 >= len(args):
            sys.exit(__doc__)
        previous_path = args[i + 1]
        del args[i:i + 2]
        if os.path.exists(previous_path):
            previous = load(previous_path)
    if len(args) != 2:
        sys.exit(__doc__)
    src, dst = args
    realm = load(src)
    save(dst, from_cluster(realm, previous) if export else to_cluster(realm))
    print("wrote " + dst)


if __name__ == "__main__":
    main(sys.argv[1:])
