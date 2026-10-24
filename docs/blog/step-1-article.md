# Step 1 - A Booking Request You Can Reproduce

Alex still does not have a reservation. [Step 0](step-0-article.md) established
who may decide eligibility, inventory, money, and safety, but offered nothing
executable. This chapter makes a smaller promise: an operator can retrieve a
synthetic booking-request fixture from PostgreSQL, stop a dependency, and observe
the difference between a live process and a usable service.

There is no model or graph. Adding one would not help us prove that we can build
the application, validate a database connection, or preserve a record across
restarts. Those are ordinary engineering responsibilities.

**Evidence status:** The Java 25 build, seven mock-based unit tests, five
Testcontainers integration tests, and PKI checks passed. Compose's internal
service path is demonstrated. Host-published Compose access and the Kubernetes
HAProxy/cert-manager path are **blocked in this environment**, not verified.
The [claim-to-evidence record](../verification/step-1-verification.md) separates
these observations. The Maven-layout correction reruns build/test/PKI and image
packaging checks. A newly authorized dedicated kind cluster is `Ready` and
contains the rebuilt booking image, but its networking preflight fails before
controller or workload deployment. No correction-time API or persistence pass is
claimed. Internal Compose observations below belong to the original candidate.
This chapter is not yet an accepted runtime milestone.

## Starting point and learning outcomes

Start at accepted `development` snapshot
`5e0c173a11d57b64d0864379e03824c282eb4543` for the original chapter. The
Maven-layout correction starts from integrated `development` at
`93c882b27a44779bbf17bbf9fb4057ce09149a5f` on
`fix/maven-service-modules`; its exact runtime fingerprint and limitations are
in the [verification record](../verification/step-1-verification.md).
The [blueprint](../plans/step-1-plan.md) records the approved correction.
No `step-01` release tag is implied.

You will learn why a fixture is not a reservation, how an injected clock makes
fixture initialization reproducible, why a failed database must not produce a
success-shaped fallback, and how to distinguish certificate trust, persistence,
and deployment readiness from a passing unit test.

Use a Java 25 JDK, Docker, Bash, OpenSSL, and Python 3. The Maven wrapper downloads
its distribution and dependencies; Testcontainers downloads pinned images.
No model account, GPU, or paid inference is needed. Deployment additionally
requires Compose or an explicitly authorized kind cluster.

| Component | Exact candidate version |
| --- | --- |
| JDK observed for compilation/tests | Temurin 25.0.5+8, no preview flags |
| Quarkus BOM and plugin | 3.40.1.1 |
| Maven / wrapper plugin | 3.9.11 / 3.3.4 |
| Compiler / Surefire and Failsafe / Enforcer | 3.14.1 / 3.5.4 / 3.6.2 |
| PostgreSQL / JDBC driver | 17.6-alpine / 42.7.13 |
| JUnit / Mockito / REST Assured / Testcontainers | 6.1.3 / 5.21.0 / 6.0.1 / 2.0.5 |
| Application container JRE | Temurin Java 25 image, digest pinned in Dockerfile |
| Docker Engine / Compose observed | 29.1.3 / 5.6.1 |
| kind / node Kubernetes | 0.33.0 / 1.35.0 |
| kubectl | 1.35.0, matching the node minor version |
| HAProxy ingress / cert-manager | 3.2.18 / 1.21.2 |

The observed lab had Linux amd64 and approximately 8 GiB memory; this is not a measured minimum.
No latency, throughput, recovery SLA, ARM, or native-image result is claimed.

The [release-pinned Quarkus datasource reference](https://github.com/quarkusio/quarkus/blob/3.40.1.1/docs/src/main/asciidoc/datasource.adoc)
describes the readiness integration, and its
[HTTP reference](https://github.com/quarkusio/quarkus/blob/3.40.1.1/docs/src/main/asciidoc/http-reference.adoc)
describes PEM TLS configuration. Executing the pinned artifacts on Java 25,
rather than reading a moving documentation page, supplies the compatibility
evidence for this limited application.

## One authoritative record, not a workflow

Before this increment there were only documents. The implemented application
has this shape:

```text
GET booking-request UUID
          |
          v
Quarkus BookingResource --> BookingRepository --> PostgreSQL fixture
          |                       |
          | 400 / 404              +-- storage failure --> explicit 503
          v
JSON: identifier, synthetic reference, stored creation instant
```

PostgreSQL owns the stored request fixture. It does not hold a confirmed
reservation or any consequential decision. There is still no durable graph
progress, model context, decision ledger, payment operation, or distributed
transaction.

Read [`BookingRequest`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRequest.java)
first: its three fields tell you the complete business scope. Then follow
[`FixtureInitializer`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/FixtureInitializer.java)
to [`BookingRepository`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingRepository.java).
Initialization creates the fixture table and inserts the known identifier in
one JDBC transaction. `ON CONFLICT (id) DO NOTHING` preserves an existing record.
It is not a migration framework or a concurrent multi-replica initialization
guarantee; service-owned schemas and migrations belong to Step 2.

Time is an input rather than an invisible dependency. The initializer receives
a `Clock`; the baseline configuration fixes it at `2026-01-02T10:00:00Z`.
[`ClockProducer`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/ClockProducer.java)
can use the system UTC clock when the optional fixed instant is absent.
Changing the clock does not rewrite a previously stored creation time.

[`BookingResource`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingResource.java)
rejects malformed UUIDs before querying storage. A canonical but missing UUID
is a 404. A failed database is not "missing": the
[`exception mapper`](../../source/booking-service/src/main/java/com/milesofsmiles/booking/BookingStorageExceptionMapper.java)
returns 503 with a stable code and logs the SQL state, without returning the
provider's exception details to the caller.

## Why introduce Maven modules after starting with one project?

The first Step 1 implementation was a single Quarkus project at the repository
root: one `pom.xml`, one `src/`, and one `target/`. That was sufficient to
establish the first executable baseline. There was only a booking fixture to
build, so a parent project and module list would not yet have separated any
applications. We keep that starting point explicit rather than pretending the
series began with a multi-service implementation.

The roadmap will introduce separately owned services in later chapters. Keeping
each new application's code in the same root `src/` would mix their dependency,
configuration, test, and packaging boundaries. Giving every service a completely
independent build is another valid choice, but here it would duplicate the
Java baseline and pinned build configuration. We choose a Maven reactor: Maven
reads a parent project's module list and builds the selected projects together.
This provides one reproducible root build while keeping each service's code and
application dependencies local.

This correction changes the build structure before the next service arrives:

```text
Original Step 1                 Corrected layout
pom.xml (Quarkus application)   pom.xml (parent and reactor, packaging=pom)
src/main/                      source/booking-service/pom.xml (application)
src/test/                      source/booking-service/src/main/
target/                        source/booking-service/src/test/
                               source/booking-service/target/
```

The parent owns the version pins, Quarkus BOM, and shared plugin configuration.
The booking module chooses its application dependencies and activates its
Quarkus and test plugins. That distinction matters: central plugin management
does not mean every future module must package a Quarkus application or inherit
all booking dependencies. The root POM is no longer a runnable application.
These are Maven build modules, not Java Platform Module System modules.

The cost is a parent POM, a module POM, and longer source and artifact paths.
Tests must resolve repository-level lab scripts explicitly, and Docker must copy
the module's output rather than root `target/`. The benefit is a clear place for
each later service without duplicating the common build policy. No fleet,
rental, or payment module is created early. A Maven module is not a runtime
service boundary by itself: the current system still runs the same booking
application and PostgreSQL, with the same read-only API and authority limits.

## Build it before deploying it

All commands run from the repository root. Point `JAVA_HOME` at a Java 25 JDK:

```bash
./mvnw clean compile
./mvnw verify
scripts/check-pki.sh
```

The build refuses Java 27. That is intentional: installing a newer JDK does not
silently approve the series' conditional future migration. The
[`pom.xml`](../../pom.xml) imports the exact Quarkus BOM, pins shared plugin
configuration, and aggregates the
[`booking-service` module](../../source/booking-service/pom.xml). The root has
`pom` packaging; only the module declares application dependencies and activates
the Quarkus and test plugins. Future code projects follow `source/<service-name>/`
and register in the root module list, not in this chapter's implementation.
The wrapper pins both Maven's version and distribution checksum.

Root commands build the whole reactor. Use
`./mvnw -pl source/booking-service -am verify` to select booking and its reactor
prerequisites. Sources and tests live under `source/booking-service/src/`;
reports, ephemeral test certificates, and the runnable `target/quarkus-app/`
live under `source/booking-service/target/`. The integration-test bootstrap
resolves the root PKI script explicitly rather than relying on Maven's working
directory. The Dockerfile copies only booking's module output while keeping
the repository root as its build context.

[`BookingBaselineTest`](../../source/booking-service/src/test/java/com/milesofsmiles/booking/BookingBaselineTest.java)
uses Mockito for clock handoff, storage mapping, invalid/missing identifiers,
transaction commit/rollback, and explicit dependency failures.
[`BookingApiIT`](../../source/booking-service/src/test/java/com/milesofsmiles/booking/BookingApiIT.java)
starts PostgreSQL through Testcontainers, bootstraps ephemeral test certificates,
and exercises the API against the real database. The tests do not require kind.

The outage test pauses PostgreSQL and checks that liveness remains 200 while
readiness and fixture retrieval fail with 503. It unpauses the dependency in a
`finally` block and checks recovery. This demonstrates bounded lab behavior,
not a recovery SLA or tolerance of arbitrary distributed failures. Pool
connections are validated on borrow so a database restart does not leave
apparently healthy but stale connections waiting for an API request.

## Local trust must be explicit

TLS encrypts a connection, but the caller also needs to know which server it
reached. [`scripts/pki.sh`](../../scripts/pki.sh) creates an ignored development
CA, server certificates, and a randomly generated local database password:

```bash
scripts/pki.sh
```

The CA key is mode `600`. Server keys and the password are mode `640` so the
explicitly selected non-root container group can read them. The CA is not
installed in the machine's trust store. Do not share `.local/step-1`.

HTTP clients use the CA and verify the requested hostname. JDBC uses
`sslmode=verify-full`, which checks the chain and PostgreSQL hostname. The
database rejects non-TLS network connections. This is server authentication,
not client-certificate authentication or mTLS.

The PKI check verifies both ingress hostnames, rejects a wrong hostname and a
future expired leaf, then renews a leaf while preserving the CA. These checks
are not evidence of a production CA lifecycle, revocation, or automatic reload.
`scripts/pki.sh renew` replaces local leaf certificates; restart the relevant
services afterward. It refuses to overwrite an existing CA during `init`.

## Compose: observe the stored request

After a successful Maven build:

```bash
export LOCAL_GID="$(id -g)"
docker build -f deploy/step-1/Dockerfile -t miles-of-smiles/booking:step-1 .
docker compose -f deploy/step-1/compose.yaml up -d --wait --wait-timeout 120
scripts/smoke.sh 8443
```

The [Compose file](../../deploy/step-1/compose.yaml) introduces only the app and
PostgreSQL. Both run non-root with resource limits. The app exposes loopback
HTTPS, has a read-only root filesystem, and reads its password from a mounted
non-committed file. PostgreSQL has no published host port and uses a named volume.

The expected fixture shape, observed in the integration tests and inside the
Compose application network namespace, is:

```json
{"id":"00000000-0000-4000-8000-000000000001","reference":"SYNTHETIC-ALEX-WEEKEND","createdAt":"2026-01-02T10:00:00Z"}
```

This output says that a synthetic request exists. It does not say that Alex is
eligible, that a car is available, or that a payment succeeded.

The smoke script verifies the fixture, health, 400, and 404 through both
`booking.127.0.0.1.nip.io` and `booking.127.0.0.1.sslip.io`. It uses
`curl --resolve` to keep DNS deterministic while retaining CA and hostname
verification. Public DNS resolution and browser access are separate concerns;
rebind protection may block these domains' loopback answers.

In this environment the published port timed out despite healthy internal
services. Running the same smoke assertions inside the app's network namespace
passed, but that is **not** a passing host-published-port walkthrough.

To distinguish persistence from deterministic recreation:

```bash
scripts/check-persistence.sh compose
```

The script first stores a database-only table comment, then restarts the
application and PostgreSQL separately. A freshly recreated fixture would not
recreate that comment. The marker survived in the observed Compose exercise;
the host-side final smoke remains blocked. Do not remove volumes to "recover"
from a failed check.

## kind: route by hostname without inventing TLS trust

The Kubernetes candidate uses this planned deployment path; end-to-end operation
is not yet demonstrated in this environment:

```text
booking.127.0.0.1.nip.io / booking.127.0.0.1.sslip.io
                       |
          loopback port-forward :8444
                       |
          HAProxy ingress: SNI routing
                       | TLS passthrough
                       v
              Quarkus app HTTPS
                       | JDBC verify-full
                       v
             PostgreSQL -- retained local PV

development CA Secret --> cert-manager Issuer --> app + PostgreSQL certificates
```

Server Name Indication (SNI) carries the requested hostname during the TLS
handshake. HAProxy uses it to route without terminating TLS. The application
presents the cert-manager-issued certificate containing both public wildcard
DNS hostnames. The [version-pinned HAProxy annotation reference](https://github.com/haproxytech/kubernetes-ingress/blob/v3.2.18/documentation/annotations.md#ssl-passthrough)
defines this mode; it is not HTTP Layer 7 inspection.

Follow the [root walkthrough](../../README.md#kind-haproxy-cert-manager-and-local-storage)
for context confirmation, pinned images, cert-manager installation, certificate
readiness, application deployment, and foreground port-forwarding. The user
authorized the dedicated cluster in this session; never interpret those
commands as permission to replace someone else's cluster.

[`certificates.yaml`](../../deploy/step-1/kind/certificates.yaml) defines a
namespaced CA Issuer and separate app/PostgreSQL Certificates.
[`storage.yaml`](../../deploy/step-1/kind/storage.yaml) binds a retained local PV
to the named node. It survives pod replacement, not node deletion, cluster
deletion, or disk failure. The local `1Gi` declaration is not a filesystem quota.

Kubernetes HTTPS probes skip certificate identity verification; they cannot
replace the smoke client's trust checks. cert-manager renews Secrets, but this
baseline requires controlled workload restarts to load renewed material.
Automatic renewal/restart behavior, CA rotation, and mTLS remain unverified.

The first cluster attempt failed because the nested node lacked `/dev/kmsg`.
Restoring that device only inside the dedicated node allowed startup. Image
downloads were then preloaded from Docker to avoid a node-side registry DNS
failure. These environment repairs do not establish a healthy pod network:
cert-manager still could not reach `10.96.0.1:443`, and the ingress path could
not be exercised. No certificate issuance, kind persistence, or ingress success
is claimed.

Further diagnosis identified why Service routing never became usable:
kube-proxy cannot insert its rules because the environment does not support
the iptables comment match (`xt_comment`). A probe with the legacy backend
fails as well. The preparation and installation scripts now call
[`check-kind-prerequisites.sh`](../../scripts/check-kind-prerequisites.sh)
before changing deployment resources; it tests an unreferenced temporary chain
and cleans it up. This check reports the missing prerequisite explicitly. It
does not substitute for a healthy CNI or authorize changing host network
security controls.

The Maven-module correction repeated the cluster exercise rather than assuming
the earlier diagnosis still applied. A fresh, explicitly authorized
`mos-step-1` cluster reached `Ready` after restoring the missing `/dev/kmsg`
device only inside its node. The rebuilt booking image was imported into that
node with the image ID recorded in the verification record. The documented
preflight again failed on the iptables comment match; kube-proxy independently
logged failed Service-rule insertion, and an isolated legacy-backend probe
failed too. The install and prepare scripts stopped before installing
cert-manager or creating chapter workloads. Thus the corrected module packages
and its image can be loaded into kind, but the normal issuer, ingress, endpoint,
and restart/persistence path remains blocked by the host kernel. Neither the
imported image nor a `Ready` control-plane node proves application behavior.

## Failure boundaries and limitations

An unavailable database produces an explicit failure, never a synthetic
success fallback. Startup initialization failure aborts application startup.
Readiness depends on PostgreSQL; liveness does not. The API has no write,
eligibility, pricing, inventory, payment, authentication, or graph capability.

The local database identity is a development bootstrap owner, not an isolated
production role. The HAProxy controller can read chapter Secrets, including
the namespaced development CA key. The application has no Kubernetes API token,
but that does not make the overall deployment a production security design.
Use synthetic data only and do not expose these services publicly.

No NetworkPolicy enforcement, HA, zero-downtime certificate rotation, immutable
audit evidence, or measured performance target is claimed. Build/test warnings
from Maven's dependencies and Mockito's dynamic agent are recorded rather than
misrepresented as preview-language requirements.

The useful result is deliberately modest: a real database-backed request
fixture with deterministic initialization and observable failure behavior.
Before accepting Step 1, complete the blocked published-port, cert-manager,
HAProxy, and kind recovery gates against a frozen candidate. Step 2, when
separately authorized, introduces service-owned records and migrations; it is
not implemented here.
