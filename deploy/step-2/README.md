# Step 2 local deployment

Run commands from the repository root. Use Java 25, Docker Engine 29.1.3,
Compose 5.6.1, Bash, OpenSSL and Python 3. The observed kind tools are
kind 0.33.0 and kubectl/Kubernetes 1.35.0 on Linux amd64 with 8 GiB RAM.
This is an observed lab, not a measured resource minimum.

The application and database base images are digest-pinned in the Dockerfile
and manifests. Application tags name locally built chapter artifacts; record
their actual image IDs for your candidate. Rebuild/reload them after source
changes. No production HA, tenant authentication, mTLS, or NetworkPolicy
enforcement is supplied.

## Why the published correction is retained

The accepted predecessor `daed623a6c32383b96b402fe4bf27f22778b060a`
integrated the Maven-module layout correction after the original Step 1
baseline. Its purpose was to keep each service's project under
`source/<service-name>/` while preserving the root reactor commands and the
existing booking behavior. The [Step 1 blueprint amendment](../../docs/plans/step-1-plan.md#approved-corrective-amendment-maven-modules)
records that scope.

Its documentation reports an attempted kind run, not a missing implementation:
the node became Ready and accepted the image, but kernel networking prevented
the chapter workloads and ingress checks from running. Build/package success
was deliberately not reported as deployment success.

Because that integration is already on published `development`, this chapter
does not amend or replace it, force-push the target, or pretend its recorded
limitations never existed. The later approved hostNetwork fallback demonstrates
a narrower node-local path without retroactively passing the earlier normal
kind/HAProxy gate. The [Step 2 article](../../docs/blog/step-2-article.md#why-the-earlier-correction-remains-in-history)
explains this incremental history for readers.

## Build and private inputs

```bash
./mvnw clean compile
./mvnw verify
scripts/step-2-prepare.sh
export LOCAL_GID="$(id -g)"
for service in booking rental fleet; do
  docker build -f deploy/step-2/Dockerfile \
    --build-arg SERVICE="$service-service" \
    -t "miles-of-smiles/$service:step-2" .
done
```

Run preparation once. It refuses to replace `.local/step-2`, creates six
separate runtime/migrator password inputs and a bootstrap password, and reuses
the existing development PKI generator without touching Step 1 inputs.
The lab shares one application server certificate across the three services.
Its `localhost` SAN is used for verified probes and forwarded client access;
this is not per-workload identity or production PKI.

Flyway 12.0.0 migrations run as standalone Java processes before applications,
not as runtime startup DDL. Each owner has its own history and credentials.
Runtime containers mount only their own password and the server-trust inputs.
The database bootstrap container necessarily receives the privileged initial
inputs. Generated keys/passwords stay ignored; do not print or commit them.

## Compose

```bash
docker compose -f deploy/step-2/compose.yaml config --quiet
docker compose -f deploy/step-2/compose.yaml up -d --wait --wait-timeout 150
scripts/step-2-smoke.sh
scripts/step-2-isolation.sh compose
scripts/step-2-persistence.sh compose
scripts/step-2-smoke.sh
```

Booking, rental and fleet publish loopback HTTPS on 8543, 8544 and 8545.
PostgreSQL has no published port. The named volume is separate from Step 1.
Migration containers must finish successfully before their runtime starts.
Bootstrap creates roles/schemas only for an empty database; restarting does not
rotate credentials or silently reinitialize existing data.

The smoke script checks actual JSON, status codes, replay headers and stable
IDs, rather than just asking if a port is open. The isolation script uses the
real runtime credentials to check all six ordered cross-owner read/write pairs,
DDL (including temporary tables), role assumption and history access. The
persistence script compares a database-only marker and stored record
IDs/hashes after separate restarts of all services and PostgreSQL.

In this sandbox, host-published Compose traffic times out. The same smoke
assertions passed over the internal Docker network with hostname/CA checking
preserved. This diagnostic requires permission to enter the chapter container's
network namespace; it is not the normal reader path or a host-port pass:

```bash
booking_ip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mos-step-2-booking-1)
rental_ip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mos-step-2-rental-1)
fleet_ip=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' mos-step-2-fleet-1)
pid=$(docker inspect -f '{{.State.Pid}}' mos-step-2-booking-1)
nsenter -t "$pid" -n env \
  BOOKING_CONNECT_TO="localhost:8543:$booking_ip:8443" \
  RENTAL_CONNECT_TO="localhost:8544:$rental_ip:8443" \
  FLEET_CONNECT_TO="localhost:8545:$fleet_ip:8443" \
  scripts/step-2-smoke.sh
```

`--connect-to` changes the transport destination, not the URL hostname that TLS
verifies. Do not substitute `curl -k`. Run this again after the persistence
exercise; inspect addresses again if containers were recreated.

## Existing kind context: normal direct-pod path

Use only the explicitly confirmed `kind-mos-step-2` context and `mos-step-2`
namespace. The apply script does not create/reset a cluster or switch the
user's kubeconfig. For this session the user separately authorized a dedicated
cluster, created with an isolated kubeconfig:

```bash
kind create cluster --config deploy/step-2/kind/cluster.yaml \
  --kubeconfig .local/step-2/kubeconfig --retain --wait 120s
export KUBECONFIG="$PWD/.local/step-2/kubeconfig"
kubectl --context kind-mos-step-2 get nodes
CONFIRM_KIND_CONTEXT=kind-mos-step-2 scripts/step-2-kind-apply.sh
```

The normal script uses direct database pod addressing through `hostAliases`
while retaining the `postgres` certificate hostname and JDBC `verify-full`.
This avoids requiring ClusterIP/DNS routing but still requires working
pod-to-pod networking. Reapply after a database pod replacement: the renderer
updates application `hostAliases` and uses the new endpoint in migration Job
names. Completed Jobs are immutable and retained; do not delete them or
persistent data merely to retry a failed lab.

PostgreSQL stores data in the dedicated node's
`/var/local/mos-step-2/postgres`, mounted with `hostPath`. The script creates
that directory as UID/GID 70 only if absent; it refuses an unexpected owner
rather than changing existing data permissions. Storage survives pod
replacement, not node/cluster deletion, and has no filesystem quota. This
single-node teaching fallback intentionally avoids depending on a provisioner
that itself needs broken Service routing.

In this environment, userspace iptables was already installed; conntrack
1.4.8 and kmod were installed during diagnosis. `modprobe -n` reports missing
`ip_tables`, `xt_comment` and `nf_conntrack` modules for kernel `6.12.8+`;
some conntrack support is already built in. Installing userspace tools cannot
provide those host modules. The isolated iptables comment-match probe fails,
kube-proxy cannot build its rules, and direct migration Job connections to the
database pod time out. Normal pod networking remains blocked.

## Approved single-node hostNetwork fallback

The user separately approved this fallback after the direct-pod path failed.
It is not a fix for CNI, ClusterIP, DNS, ingress, or cross-node traffic.
All services and PostgreSQL share the dedicated node's network namespace.
PostgreSQL listens only on node loopback, and the services bind loopback HTTPS
on distinct ports. No HAProxy, cert-manager, host firewall change, or insecure
TLS option is involved.

Once the dedicated cluster is usable:

```bash
export KUBECONFIG="$PWD/.local/step-2/kubeconfig"
CONFIRM_KIND_CONTEXT=kind-mos-step-2 STEP2_NETWORK=host \
  scripts/step-2-kind-apply.sh
CONFIRM_KIND_CONTEXT=kind-mos-step-2 scripts/step-2-isolation.sh kind
```

The renderer applies plain manifests through direct `kubectl`, changes network
settings and probe ports explicitly, and gives migration Jobs separate `-host`
names. After importing locally built images, it fingerprints their image IDs in
service pod templates and migration Job names. Migration Job names also include
the database image and current database pod address, so a database replacement
does not try to mutate an old immutable Job. Reapplying after a rebuild thus
rolls changed services and runs a new migration Job even though the lab tag stays
`:step-2`; previous Jobs remain as evidence. An unchanged image revision does
not cause an unnecessary rollout. Failed normal Jobs remain as failure evidence.
Services use `Recreate` because two replicas cannot bind the same node port.
There is no HA claim.

In three separate terminals, keep these commands in the foreground:

```bash
kubectl --context kind-mos-step-2 -n mos-step-2 port-forward --address 127.0.0.1 deployment/booking 8643:8443
kubectl --context kind-mos-step-2 -n mos-step-2 port-forward --address 127.0.0.1 deployment/rental 8644:8444
kubectl --context kind-mos-step-2 -n mos-step-2 port-forward --address 127.0.0.1 deployment/fleet 8645:8445
```

These local ports avoid the Compose publications. In a fourth terminal:

```bash
BOOKING_URL=https://localhost:8643 RENTAL_URL=https://localhost:8644 \
  FLEET_URL=https://localhost:8645 scripts/step-2-smoke.sh
CONFIRM_KIND_CONTEXT=kind-mos-step-2 scripts/step-2-persistence.sh kind
```

Pod replacement ends port-forwards. Reopen all three and run the smoke command
again. The observed fallback passed both before and after application/database
replacement, with verified HTTPS/JDBC and retained marker/records.

### Dedicated-node startup limitation observed here

The initial kind command failed because the nested node lacked `/dev/kmsg`.
The retained node was repaired **only inside that explicitly authorized node**:
create the missing character device (major 1, minor 11), restrict its permissions,
restart kubelet, and resume `kubeadm init` using `/kind/kubeadm.conf` without a
reset. Export the isolated kubeconfig afterward. Do not run this recipe on
someone else's node or interpret it as a general requirement.

The shipped `/kind/manifests/default-cni.yaml` required rendering its
`{{ .PodSubnet }}` placeholder as `10.244.0.0/16`. The default CNI/storage
manifests were installed to finish the interrupted bootstrap, and the
control-plane scheduling taint removed only on this dedicated single-node lab.
kindnet needed `KUBERNETES_SERVICE_HOST` set to the actual node IP and
`KUBERNETES_SERVICE_PORT=6443` to reach the API without ClusterIP routing.
The node then became Ready, but that did not establish pod-network usability.
The storage provisioner still could not reach the API from its pod; the final
chapter deployment does not depend on it.

The apply script uses platform-specific digest-preserving image imports.
An earlier unqualified `import-<date>` reference required adding its normalized
Docker name for containerd; no image bytes or pinned digest were replaced.
The final import supplies `--base-name docker.io/library/postgres` to avoid that
unnamed-reference issue. These are environment recovery observations, not a
portable successful normal-cluster walkthrough.

## Upgrade and preservation boundaries

The automated upgrade evidence uses PostgreSQL Testcontainers: recreate the
exact Step 1 fixture table, change its reference/time to nondefault synthetic
values, run the production owner bootstrap, then migrate with booking's owner
identity. The row survives and repeat migration is a no-op; checksum drift
fails. Separate tests verify clean installation for every owner.

A live upgrade requires a backup and a confirmed existing database target.
`bootstrap.sql` is a **one-time administrator operation**, not a runtime or
repeatable seeding script: it creates roles/schemas, removes public creation
and temporary-table privileges, installs `btree_gist`, and transfers the legacy
fixture's ownership if present. The booking owner migration moves that table
without recreating rows. It does not adopt an arbitrary incompatible schema.
Stop the old fixture initializer before changing its ownership.

This chapter does not automatically attach a new stack to a user's Step 1
volume, run a live migration against it, reset passwords, downgrade schemas,
or promise zero-downtime upgrades. Migration failures require inspection of the
transaction/history and explicit recovery; never use volume deletion, Flyway
clean/repair, or baseline-on-migrate to manufacture a pass.

Stop this chapter with `docker compose -f deploy/step-2/compose.yaml stop` and
Ctrl-C for port-forwards. Preserve the kind node, named volume and any existing
PVCs. Do not use `down -v`, cluster deletion, or storage cleanup.
