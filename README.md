# Miles of Smiles

An incremental rental-system teaching project. Start with the
[chapter index](docs/blog/index.md), [rental story](story.md), and
[series contract](plan.md).

Step 0 is accepted. Step 1 is an implementation candidate: its Java build,
mock-based unit tests, PostgreSQL Testcontainers tests, and development PKI checks
pass. Compose runs internally, but published-port access and the kind
controller/ingress walkthrough are blocked in the current environment. See the
[verification record](docs/verification/step-1-verification.md) before treating
any deployment promise as verified.
The current Maven-layout correction rechecks the build, tests, PKI, and image
packaging. Its newly authorized kind cluster is `Ready` and contains the rebuilt
booking image, but the networking prerequisite fails before chapter deployment.
The internal Compose observations are historical, not a fresh deployment pass.

## Build from the repository root

Use a Java 25 JDK, Docker with a running daemon, Bash, OpenSSL, Python 3, and
`unzip`.
Set `JAVA_HOME` to Java 25; the build rejects Java 27 rather than silently
changing the chapter baseline. No model credentials or kind cluster are needed
for the standard build.

```bash
./mvnw clean compile
./mvnw verify
scripts/check-pki.sh
```

Maven 3.9.11 is downloaded by the checksum-pinned wrapper. The first build needs
network access to Maven Central and the pinned container registry images.
`verify` runs seven mock-based unit tests and five PostgreSQL integration tests,
including a database outage, recovery, verified JDBC TLS, and HTTPS trust
rejection. Test reports, packaged application, and ephemeral test certificates
stay under ignored `source/booking-service/target/`; the standalone PKI check
uses root `target/`.

## Maven service layout

The root [`pom.xml`](pom.xml) is the parent and reactor aggregator. It pins the
Java baseline, Quarkus BOM, and shared plugin configuration without packaging an
application. Each code project is a Maven module under `source/<service-name>/`.
Only [`source/booking-service`](source/booking-service/pom.xml) exists today;
future services are added only in their authorized chapters.

```text
pom.xml                         parent and module list
source/booking-service/pom.xml   booking dependencies and application plugins
source/booking-service/src/      Java sources, configuration, and tests
source/booking-service/target/   module-local generated output (ignored)
deploy/step-1/                   chapter deployment resources
scripts/                        repository-level lab tooling
```

The root compile/verify commands build every registered module. To work only on
booking, run these from the repository root:

```bash
./mvnw -pl source/booking-service -am verify
```

Docker builds still use the repository root as context and copy the booking
module's `target/quarkus-app/`. Java and test Javadocs describe the current
fixture, failure, and TLS boundaries; they do not promise future capabilities.

The observed lab is Linux amd64 with Temurin 25.0.5+8, Docker 29.1.3 and 8 GiB
memory. This is the tested environment, not a measured minimum or performance
target. Java 27, ARM, native images, and production deployment are not qualified.
If your network requires a private CA, use your organization's approved Java
trust store. Do not disable Maven certificate verification.

## Compose: an application and a database

Install Docker Compose v5.6.1. Generate development-only inputs once:

```bash
scripts/pki.sh
export LOCAL_GID="$(id -g)"
docker build -f deploy/step-1/Dockerfile -t miles-of-smiles/booking:step-1 .
docker compose -f deploy/step-1/compose.yaml up -d --wait --wait-timeout 120
scripts/smoke.sh 8443
scripts/check-persistence.sh compose
```

The API is available at
`https://booking.127.0.0.1.nip.io:8443/api/bookings/00000000-0000-4000-8000-000000000001`.
The equivalent `booking.127.0.0.1.sslip.io` route has the same certificate SAN.
The smoke script uses `curl --resolve` for deterministic loopback resolution
while still validating the requested hostname and CA. Browser access depends
on your DNS resolver allowing these public wildcard names; DNS rebinding filters
may reject loopback answers. Neither DNS service supplies TLS trust.

Use `--cacert .local/step-1/ca.crt` with curl. The CA is never installed in
system trust. The application listens only on HTTPS; the published port is
loopback-only, and PostgreSQL has no host-published port. JDBC uses
`sslmode=verify-full`. Generated CA keys, database password, and certificates are
under ignored `.local/step-1/`; do not commit or share them.

The database uses a named persistent volume. The persistence exercise adds a
synthetic table comment, then restarts the application and database separately.
Checking that database-only marker prevents deterministic fixture recreation
from masquerading as persistence. It leaves the marker in place.

Renew certificates without replacing the CA or password:

```bash
scripts/pki.sh renew
docker compose -f deploy/step-1/compose.yaml restart postgres
docker compose -f deploy/step-1/compose.yaml up -d --wait --wait-timeout 120 postgres
docker compose -f deploy/step-1/compose.yaml restart app
docker compose -f deploy/step-1/compose.yaml up -d --wait --wait-timeout 120
scripts/smoke.sh 8443
```

Renewal requires a controlled restart; this baseline does not claim automatic
certificate reload. Keep the private CA key mode `600`; server keys/password
are group-readable (`640`) for the explicitly selected container group.
The containers run as non-root. On non-root hosts, retain `LOCAL_GID` for every
Compose invocation. Stop without deleting data:

```bash
docker compose -f deploy/step-1/compose.yaml stop
```

## kind: HAProxy, cert-manager, and local storage

This lab is pinned to kind v0.33.0, Kubernetes v1.35.0, kubectl v1.35.0,
HAProxy Kubernetes Ingress v3.2.18, and cert-manager v1.21.2.
The client and server minor versions match; do not infer compatibility with
arbitrary later Kubernetes clients or nodes.

The user authorized a dedicated `mos-step-1` cluster for this session. Other
readers must obtain permission before this optional creation command; an
existing cluster must not be replaced:

```bash
kind create cluster --config deploy/step-1/kind/cluster.yaml
kubectl --context kind-mos-step-1 get nodes
```

The chapter resources target the named single node and namespace
`mos-step-1`. They are not a generic multi-node or shared-production installer.
Confirm the context and chapter namespace before applying. Controller/CRD
installation, a StorageClass, a PV, and ingress metadata RBAC are cluster-scoped
exceptions authorized for this dedicated lab.

After the Maven build, image build, and `scripts/pki.sh`:

```bash
export CONFIRM_KIND_CONTEXT=kind-mos-step-1
scripts/check-kind-prerequisites.sh
scripts/kind-load-images.sh
scripts/install-cert-manager.sh
scripts/kind-prepare.sh
kubectl --context kind-mos-step-1 -n mos-step-1 get issuer,certificate,pvc,pods,ingress
kubectl --context kind-mos-step-1 -n mos-step-1 port-forward service/haproxy 8444:443 --address 127.0.0.1
```

The prerequisite probe creates and removes an unreferenced iptables chain
inside the dedicated kind node. It fails explicitly if the kernel lacks the
comment match needed by kube-proxy. The current environment fails this probe;
kube-proxy logs `Extension comment revision 0 not supported` and
`RULE_INSERT failed`. Neither the nft nor legacy iptables backend supports the
required match here, so retrying cert-manager installation cannot fix the
missing Kubernetes Service routing. Use a kind-capable Docker host rather than
disabling networking/security controls or interpreting node readiness as
application readiness. Passing this probe alone does not prove CNI operation.

Keep port-forwarding in the foreground. In a second terminal:

```bash
scripts/smoke.sh 8444
export CONFIRM_KIND_CONTEXT=kind-mos-step-1
scripts/check-persistence.sh kind
```

Both `booking.127.0.0.1.nip.io:8444` and
`booking.127.0.0.1.sslip.io:8444` target the HAProxy Service. HAProxy routes TLS
by SNI in passthrough mode; the application, not HAProxy, terminates HTTPS with
a cert-manager-issued certificate. PostgreSQL gets a separate certificate,
verified by the application through JDBC. This is server TLS, not mTLS.
Kubernetes HTTPS health probes do not validate certificate identity; the
separate client smoke check is required.

The namespaced `development-ca` Issuer uses the locally generated CA Secret.
cert-manager renews server Secrets; restart the PostgreSQL and application
Deployments after Secret renewal, then rerun the smoke and persistence checks.
Automatic reload, CA rotation, revocation, and renewal under outage are not
demonstrated. HAProxy can read chapter Secrets, including this development CA
key: this dedicated synthetic lab is not a production PKI isolation design.
The application service account has no API permissions or mounted API token.

The retained local PV is `/var/local/mos-step-1/postgres` **inside the kind node**.
It survives pod replacement, not node/cluster deletion or disk loss. The `1Gi`
PV declaration is a capacity advertisement, not a filesystem quota. Never delete
the node, namespace, PVC, or volume as a cleanup step. Stateful services are
single-instance and non-HA; NetworkPolicy enforcement is not claimed.

## API boundary

`GET /api/bookings/{id}` returns one stored synthetic request, 400 for a
malformed UUID, 404 for an absent request, and 503 for a storage failure.
It does not confirm a reservation, check eligibility, price a rental, allocate
a car, take payment, or provide authentication. Do not expose it to the internet.
All future capabilities remain in the [roadmap](plan.md#roadmap).
