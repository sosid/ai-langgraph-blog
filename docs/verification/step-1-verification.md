# Step 1 Verification - Reproducible Booking Baseline

**Result: implementation candidate; chapter acceptance blocked.** Build,
deterministic mock/PostgreSQL tests, isolated PKI, and internal Compose
observations pass. Host-published Compose access and end-to-end Kubernetes
controller/issuer/ingress/recovery gates do not pass in this environment.
Neither installed resources nor a Ready node establish those promises.

## Frozen candidate and authority

| Field | Evidence |
| --- | --- |
| Accepted predecessor | `5e0c173a11d57b64d0864379e03824c282eb4543`, clean `development` before this task |
| Step 0 acceptance | User explicitly confirmed Step 0 complete; earlier review findings were not reopened |
| Blueprint | `docs/plans/step-1-plan.md`, approved by the user before implementation |
| Branch | Local `features/step-1`, created from the accepted predecessor |
| Runtime source fingerprint | SHA-256 `b191b8f512a458817ef74f5346f377d0cd84e992f1ad5d0f12bc893519392ea4` |
| Fingerprint definition | SHA-256 of sorted `file-sha256`, two spaces, POSIX relative path, newline records for `pom.xml`, `mvnw`, `mvnw.cmd`, `.dockerignore`, and all regular files under `.mvn/`, `src/`, `scripts/`, `deploy/`; 36 files |
| Working tree at verification | Uncommitted implementation and documentation; existing Step 0 source/article unchanged |
| Release actions | The user subsequently authorized committing, pushing, and opening a draft PR for review despite the documented blockers; merge, tag, and chapter acceptance remain unauthorized |

The runtime fingerprint is independent of this record, avoiding a
self-referential hash. The final complete worktree tree identifier can be
computed through a temporary Git index without creating a commit; it is reported
at handoff. Relevant changes invalidate the associated checks.

The user separately authorized the dedicated `mos-step-1` kind cluster and
required HAProxy ingress, cert-manager issuance, nip.io and sslip.io routes, and
allowed local persistent storage. The approved blueprint records this amendment.

## Findings and limitations first

| Finding | Affected claim | Evidence / disposition |
| --- | --- | --- |
| Host-to-Compose published HTTPS times out | Complete Compose walkthrough | `scripts/smoke.sh 8443` exits 28; internal app namespace smoke and service-to-service TLS succeed |
| kind pod network cannot reach API Service | Certificate issuance and complete Kubernetes path | cert-manager logs `dial tcp 10.96.0.1:443: i/o timeout`; webhook rollout times out |
| kube-proxy cannot program Service rules | Required host kernel/network prerequisite | `iptables`: `Extension comment revision 0 not supported`, `RULE_INSERT failed`; legacy comment-match probe fails too; final preflight reports blocked before controller/resource changes |
| Issuer/webhook prerequisite is unavailable | kind certificate/API/persistence/renewal acceptance | The preparation path cannot establish certificate readiness; do not advance to an asserted successful workload/ingress result |
| Public wildcard DNS did not resolve in this environment | Browser/DNS demonstration | Smoke assertions use explicit `--resolve`; this verifies hostname/TLS on the tested path, not public DNS availability |
| Manual security review only | Security status | Credential patterns and code/configuration reviewed; no independent penetration test, production qualification, or external security audit |
| Dedicated local CA shares chapter Secret scope | PKI authority boundary | HAProxy's namespaced secret informer can read the development CA key; documented synthetic local-lab limitation, not a production trust isolation design |

An initial PostgreSQL test key ownership failure was fixed by running the
test server as UID/GID 70 and copying the generated key as that identity.
An initial restart probe exposed stale pooled connections; validation on borrow
was added and the affected Maven/Compose checks rerun. An attempted simultaneous
app/database certificate restart caused startup connection refusal; the
walkthrough now waits for PostgreSQL before restarting the app. The corrected
ordered recovery and served app certificate were checked.

## Environment and exact pins

Observed Linux amd64, kernel 6.12.8+, approximately 8 GiB RAM; Docker Engine
29.1.3, Compose 5.6.1, OpenSSL from the environment, Temurin 25.0.5+8.
Maven 3.9.11 and wrapper 3.3.4; Quarkus BOM/plugin 3.40.1.1; compiler 3.14.1,
Surefire/Failsafe 3.5.4, Enforcer 3.6.2.
Resolved JDBC 42.7.13, JUnit 6.1.3, Mockito 5.21.0,
REST Assured 6.0.1, Testcontainers 2.0.5.

The initial shell supplied Java 27, which was not used as the code baseline.
Java 25 and missing tools were restored in session storage. Maven initially
rejected the environment's intercepted TLS chain; using the pre-existing,
approved environment Java trust store through `MAVEN_OPTS` resolved it without
disabling certificate verification or altering repository trust settings.

kind 0.33.0 uses Kubernetes 1.35.0; final kubectl is checksum-verified 1.35.0.
An initially downloaded 1.37.1 client was replaced to align the minor version.
HAProxy Kubernetes Ingress is 3.2.18; cert-manager is 1.21.2.

| Image | Immutable digest |
| --- | --- |
| PostgreSQL 17.6-alpine | `sha256:ef257d85f76e48da1c64832459b59fcaba1a4dac97bf5d7450c77753542eee94` |
| Temurin Java 25 JRE base | `sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f` |
| kind node 1.35.0 | `sha256:4613778f3cfcd10e615029370f5786704559103cf27bef934597ba562b269661` |
| HAProxy ingress 3.2.18 | `sha256:edcf448e0439b7ac13a5964813ee1227cee04fc0a6bbb943d4408a243f7cc39a` |
| cert-manager controller | `sha256:70f532fd9cfde0b09d55687200942399d89838bc2d5d5b45152eb799a15912b8` |
| cert-manager cainjector | `sha256:c85268c64f2e0e76684bf5fe8906caff34b82523561c6affe0fae3546bd87562` |
| cert-manager webhook | `sha256:a60e2dac46dbb8a7f3df95c54ce941012f54c2fe022f0ee55aaa1ab40ed957ae` |

Final local application image ID:
`sha256:f7b8a1d907bd583e29d1155efbf74234b8d9df101ab2c9e1e4c27fdaa960bbc2`.
The local `step-1` tag is not a published immutable release; rebuild it for a
changed candidate. Versions and digests are not performance or portability
guarantees.

## Claim-to-evidence inventory

| Article claim / promise | Exact implementation and check | Classification |
| --- | --- | --- |
| No graph/model/consequential booking behavior | `pom.xml`; read-only `BookingResource` and three-field `BookingRequest` | Verified by source review; future behavior excluded |
| Java 25 build | Enforcer Java `[25,26)`, compiler release 25, checksum-pinned wrapper; root compile/verify | Verified at runtime fingerprint |
| Stored synthetic request and stable clock | `ClockProducer`, `FixtureInitializer`, `BookingRepository.initialize/find`; unit and PostgreSQL tests | Verified at runtime fingerprint |
| Existing fixture is not overwritten | SQL `ON CONFLICT DO NOTHING`; integration test with a different later instant | Verified at runtime fingerprint |
| Parameterized read; canonical input validation | Prepared statement UUID parameter; malformed/noncanonical 400 and absent UUID 404 tests | Verified at runtime fingerprint |
| Explicit dependency failure | `BookingStorageExceptionMapper`; unit provider-detail rejection and real paused PostgreSQL API 503 | Verified at runtime fingerprint |
| Live process is distinct from readiness | SmallRye datasource readiness; outage test live 200/ready 503 and recovery | Verified for tested lab, no SLA |
| Mock-based unit coverage | Seven tests in `BookingBaselineTest` | Passed, no skips |
| PostgreSQL integration coverage | Five tests in `BookingApiIT`, `PostgresResource` | Passed, no skips |
| HTTP/JDBC server trust | `HealthProbe`, HTTPS integration assertion, `verify-full` JDBC query of `pg_stat_ssl`, wrong-trust rejection | Verified for these clients/paths |
| PKI permissions/hostname/expiry/renewal | `scripts/pki.sh`, `scripts/check-pki.sh` | Passed; not production CA qualification |
| Compose non-root, no host database port, app loopback publication | Dockerfile, Compose configuration, actual running container configuration | Source/configuration and internal execution inspected |
| Compose exact fixture and health | `nsenter -t <app-pid> -n scripts/smoke.sh 8443` | Passed inside the app network namespace only |
| Compose service-to-service TLS | A non-root temporary client container used `HealthProbe https://app:8443/q/health/ready` with the mounted CA | Passed; not host publication |
| Storage survives app/database restart | Synthetic `COMMENT ON TABLE`, separate restarts and container recreation, marker query and internal smoke | Observed pass; complete documented host smoke blocked |
| Deployed certificate renewal | CA-preserving leaf renewal, ordered PostgreSQL/app restart, served app serial compared with renewed certificate, internal smoke | Observed pass internally; host path blocked |
| Both wildcard DNS hostname SANs | PKI check; internal smoke with `--resolve` for nip.io and sslip.io | Verified SAN/client behavior; public DNS unverified |
| HAProxy/cert-manager/local PV path | `deploy/step-1/kind/*`, install/load/prepare scripts, client schema checks, attempted installation | Implemented but full gate blocked |
| kind recovery and certificate issuance | Requires ready webhook, issued certificates, workload/API/ingress smoke and restarts | Blocked; not inferred from manifests |
| Cumulative promises | Existing Step 0 preserved; `P-BOOK-BASELINE` split into `P1-*` entries | Updated; aggregate baseline remains blocked |

The diagrams are plain text, inspected for ownership and transport accuracy;
no Mermaid renderer or browser-rendering result is claimed. Snippets and
identifiers correspond to candidate source. Expected API JSON was observed in
tests/internal Compose, not invented as a published-port result.

## Commands and observed results

Commands below run from the repository root unless explicitly noted.

| Command / exercise | Result |
| --- | --- |
| `./mvnw clean compile` under Java 25 | Exit 0 |
| `./mvnw verify` | Exit 0; 7 unit + 5 integration tests, 0 failures/errors/skips |
| `scripts/check-pki.sh` | Exit 0; wrong hostname/expired leaf intentionally rejected; CA-preserving renewal passed |
| `bash -n scripts/*.sh deploy/step-1/app-entrypoint.sh` | Exit 0 |
| `docker compose -f deploy/step-1/compose.yaml config --quiet` | Exit 0 |
| `docker build -f deploy/step-1/Dockerfile -t miles-of-smiles/booking:step-1 .` | Exit 0; final image ID above |
| Compose `up -d --wait --wait-timeout 120` | Exit 0; app and PostgreSQL internally healthy |
| `scripts/smoke.sh 8443` on the host | Exit 28, TLS connection timeout; gate blocked |
| Same smoke script through app network namespace | Exit 0; both hostname/health/API/400/404 assertions pass |
| Separate Compose app/database restarts and recreation | Database-only marker retained; internal smoke passes after final connection-validation fix |
| Leaf renewal and ordered service restart | Renewed app serial served; internal smoke passes |
| Authorized kind creation | First attempt failed; retry succeeded after node-local `/dev/kmsg` repair; node Ready |
| `scripts/kind-load-images.sh` with explicit context confirmation | Exit 0; digest-preserving platform-specific imports |
| `scripts/install-cert-manager.sh` | Pinned manifest/checksum, images and resource configuration applied; webhook rollout times out, exit 1 |
| `kubectl --context kind-mos-step-1 apply --dry-run=client -f ...` for chapter resource files | Exit 0; validates resource shapes, not runtime routing/issuance/storage |
| Initial `scripts/kind-prepare.sh` | Exit 1: webhook validation timeout; namespaced inputs and retained PV/PVC created, but Issuer/Certificates not created |
| Final `scripts/check-kind-prerequisites.sh`, install and prepare entry points | Fail fast with the missing kernel comment-match diagnostic; probe chain cleaned up; no fake success or weakened network controls |
| Local Markdown paths/anchors and whitespace | 15 Markdown files, 54 local links/anchors passed; shell syntax and candidate whitespace checks pass |
| Credential-pattern scan | 53 tracked/candidate files passed; generated private inputs are ignored; not a comprehensive secret audit |
| Editorial and scope review | Manual pass; unsupported external/published-path claims explicitly labeled blocked |

The first kind image load hit Docker 29's incomplete multi-architecture archive
behavior. `kind-load-images.sh` uses the full local archive but imports only the
node's Linux amd64 platform with digest names; it does not relabel unrelated
content as the pinned digest. The Docker/node namespace repair and image preload
are environment observations, not application recovery features.

Build output contains dependency warnings about `sun.misc.Unsafe`, Mockito's
dynamic agent, and Testcontainers/JNA native access. Builds pass without preview
flags; no suppression is presented as a safety guarantee.

## Review, safety, and remaining gates

Manual source/editorial review checked the approved Step 1 scope, source-backed
snippets, safe outcomes, explicit database error reporting, parameterized SQL,
non-root containers, loopback publication, key permissions, and ignored private
material. This is not an independent review or a runtime vulnerability audit.

No real PII, model credentials, cloud spend, private chain-of-thought, payment,
inventory decision, or future-step graph capability was introduced. Existing
clusters/data were not reset; no PVC or retained volume was deleted. The
dedicated kind node and Compose state are preserved. Secret output and full
provider exceptions are not returned by the fixture API.

The final source fingerprint includes the fail-fast kernel prerequisite check.
Both required Maven commands and all 12 tests were rerun after that change,
along with the PKI rejection/renewal check. The final install/prepare entry points
stop before mutation with the kernel diagnostic. The temporary probe chains are
removed. No unsupported kernel feature was simulated, no host firewall policy
was weakened, and no controller result was fabricated to meet acceptance.

Acceptance remains blocked until a healthy environment runs the documented host
Compose path and kind issuer/HAProxy path, verifies both hostname routes,
certificate use and renewal/restarts, and proves persistent state across
workload/database replacement. Rerun affected checks on that frozen candidate.
A user-authorized draft PR records this partial evidence for review; it is not a
verified chapter PR or permission to merge, publish a tag, or start Step 2.
