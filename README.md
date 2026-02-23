# Distributed Banking System on Kubernetes

A fault-tolerant distributed banking service built in Java, progressing from a
centralized in-memory store to a containerized Kubernetes deployment on Google Cloud.

## Architecture

```
REST Client (curl / test.sh)
        │
        ▼
Server.java  ─  Spark micro-framework, port 8080
        │
        ├── BaseBank      (HashMap, single node)
        └── DistributedBank
                │
                └── Infinispan DIST_SYNC cache  ──  pessimistic transactions
                        │
                  JGroups protocol stack
                  ├── TCP multicast   (local dev)
                  └── DNS_PING        (Kubernetes, via headless service)
```

Nodes discover each other in Kubernetes through a headless `jgroups` service
(`jgroups.default.svc.cluster.local`), which avoids relying on IP multicast —
disabled by default on cloud VPCs.

Transfers lock accounts in deterministic ID order to prevent deadlocks, then commit
or roll back atomically via Infinispan's `TransactionManager`.

## Stack

- **Java 17** — application runtime
- **Spark** (micro-framework) — REST endpoints
- **Infinispan 14** — distributed transactional cache with pessimistic locking
- **JGroups** — cluster membership and inter-node communication
- **Protocol Buffers / ProtoStream** — serialization across nodes
- **Docker** — container image
- **Kubernetes (GKE)** — multi-node orchestration

## Project layout

```
src/
  main/
    java/eu/tsp/transactions/
      Bank.java                   ← interface
      Account.java                ← Protobuf-annotated POJO
      BankFactory.java
      Server.java                 ← REST layer (Spark)
      base/BaseBank.java          ← centralized HashMap impl
      distributed/
        DistributedBank.java      ← Infinispan impl
        AccountSchemaBuilder.java ← ProtoStream schema generator
    docker/Dockerfile
    bin/
      run.sh                      ← container entrypoint
      image.sh                    ← build & push to Docker Hub
    resources/
      default-jgroups-tcp.xml     ← local multicast discovery
      default-jgroups-google.xml  ← Kubernetes DNS discovery
  test/
    java/.../BaseBankTest.java
    bin/
      test.sh                     ← benchmark frontend
      bank_functions.sh           ← curl wrappers
      utils_functions.sh          ← kubectl utilities
      exp.config                  ← deployment configuration
      templates/
        transactions.yaml.tmpl    ← K8s pod template
        transactions-service.yaml.tmpl
```

## REST API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/:id` | Create account |
| `GET` | `/:id` | Get balance |
| `PUT` | `/:from/:to/:amount` | Transfer amount |
| `POST` | `/clear/all` | Clear all accounts |

## Building

```bash
mvn clean package -DskipTests
```

## Running locally

```bash
java -cp target/transactions-1.0.jar:target/lib/* eu.tsp.transactions.Server
# REST API on http://localhost:8080
```

## Docker

Log in to Docker Hub, then build and push:

```bash
./src/main/bin/image.sh [TAG]
# default tag: debug-v1
```

## Kubernetes deployment

### Prerequisites

- A running GKE (or compatible) cluster
- `kubectl` configured with the cluster context
- Docker image pushed to Docker Hub

### Configure

Edit `src/test/bin/exp.config`:

```
context=<your-kubectl-context>
image=<your-dockerhub-username>/transactions:latest
nodes=3
```

### Deploy and test

```bash
# Spin up pods and the LoadBalancer service
./src/test/bin/test.sh -create

# Populate accounts
./src/test/bin/test.sh -populate

# Run concurrent random transfers
./src/test/bin/test.sh -concurrent-run

# Verify consistency: sum of all balances must equal 0
./src/test/bin/test.sh -check

# Tear down
./src/test/bin/test.sh -delete
```

### JGroups discovery modes

| Config file | Discovery | When to use |
|---|---|---|
| `default-jgroups-tcp.xml` | IP multicast (MPING) | Local Docker / single machine |
| `default-jgroups-google.xml` | DNS_PING via K8s headless service | GKE / any cloud K8s |

The container entry point (`run.sh`) selects the correct config automatically based on
the `IP` environment variable injected by Kubernetes.

### GCS-based discovery (optional)

JGroups can alternatively use a GCS bucket as a gossip router. Pass credentials via
environment variables when deploying:

```
BUCKET=your-bucket-name
BUCKET_KEY=your-gcp-key
BUCKET_SECRET=your-gcp-secret
```

> For production use, store these as Kubernetes Secrets rather than plaintext
> environment variables in the pod spec.

## Running tests

```bash
mvn test
```
