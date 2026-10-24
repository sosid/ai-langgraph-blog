#!/usr/bin/env python3
"""Render the approved node-local fallback without changing TLS identity checks."""
import json
import re
import sys

mode, database_ip, image_revision, migration_revision = sys.argv[1:]
if mode not in ("pod", "host"):
    raise SystemExit("Network mode must be pod or host")
if not re.fullmatch(r"[0-9a-f]{32}", image_revision):
    raise SystemExit("Image revision must be a 32-character lowercase hex digest")
if not re.fullmatch(r"[0-9a-f]{32}", migration_revision):
    raise SystemExit("Migration revision must be a 32-character lowercase hex digest")
remaining = sys.stdin.read().lstrip()
decoder = json.JSONDecoder()
items = []
while remaining:
    resource, end = decoder.raw_decode(remaining)
    items.extend(resource["items"] if resource["kind"] == "List" else [resource])
    remaining = remaining[end:].lstrip()
if not items:
    raise SystemExit("No Kubernetes resources to render")
document = {"apiVersion": "v1", "kind": "List", "items": items}
for item in items:
    if item["kind"] not in ("Deployment", "Job"):
        continue
    template = item["spec"]["template"]
    template.setdefault("metadata", {}).setdefault("annotations", {})[
        "miles-of-smiles.io/image-revision"
    ] = image_revision
    pod = template["spec"]
    if mode == "host":
        pod["hostNetwork"] = True
        pod["dnsPolicy"] = "Default"
        if item["kind"] == "Deployment":
            item["spec"]["strategy"] = {"type": "Recreate"}
        else:
            item["metadata"]["name"] += "-host"
    if item["kind"] == "Job":
        item["metadata"]["name"] += f"-{migration_revision}"
    if mode == "pod":
        if item["metadata"]["name"] != "postgres":
            pod["hostAliases"] = [{"ip": database_ip, "hostnames": ["postgres"]}]
        continue
    container = pod["containers"][0]
    if item["metadata"]["name"] == "postgres":
        container["args"][0] += " -c listen_addresses=127.0.0.1"
        continue
    for variable in container["env"]:
        if variable["name"] == "DB_URL":
            variable["value"] = variable["value"].replace("//postgres:", "//localhost:")
    if item["kind"] == "Deployment":
        port = {"booking": 8443, "rental": 8444, "fleet": 8445}[item["metadata"]["name"]]
        container["env"] += [
            {"name": "QUARKUS_HTTP_HOST", "value": "127.0.0.1"},
            {"name": "QUARKUS_HTTP_SSL_PORT", "value": str(port)},
        ]
        container["ports"] = [{"containerPort": port}]
        for probe in ("readinessProbe", "livenessProbe"):
            container[probe]["exec"]["command"] = [
                value.replace(":8443/", f":{port}/")
                for value in container[probe]["exec"]["command"]
            ]
json.dump(document, sys.stdout)
