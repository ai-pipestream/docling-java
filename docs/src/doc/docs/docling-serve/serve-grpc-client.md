# Docling Serve gRPC Client

[![docling-serve-grpc version](https://img.shields.io/badge/docling--serve--grpc_v{{ gradle.project_version }}-orange)](https://docling-project.github.io/docling-java/{{ gradle.project_version }}/docling-serve/serve-grpc-client)

The `docling-serve-grpc` module provides a gRPC interface for communicating with a
[Docling Serve](https://github.com/docling-project/docling-serve) backend.

It wraps the framework‑agnostic `DoclingServeApi` from [`docling-serve-api`](serve-api.md) and exposes it via gRPC, providing strongly‑typed Protobuf definitions for the complete Docling document structure.

If you prefer a standard HTTP implementation, see the reference client:
- Docling Serve Client: [`docling-serve-client`](serve-client.md)

## When to use this module

- You want to leverage gRPC for communication, benefiting from binary serialization and efficient streaming.
- You want to store the output as a protocol buffer record for archival purposes.
- You want to manage the protobuf in a schema manager to ensure non-breaking changes.
- You need a strongly‑typed contract (Protobuf) that is shared across different languages and services.
- You want to use the "Watch" pattern where the server manages the polling loop for asynchronous tasks.

## Installation

Add the gRPC client dependency to your project.

=== "Gradle"

    ``` kotlin
    dependencies {
      implementation("{{ gradle.project_groupId }}:{{ gradle.serve_grpc_artifactId }}:{{ gradle.project_version }}")
    }
    ```

=== "Maven"

    ``` xml
    <dependency>
      <groupId>{{ gradle.project_groupId }}</groupId>
      <artifactId>{{ gradle.serve_grpc_artifactId }}</artifactId>
      <version>{{ gradle.project_version }}</version>
    </dependency>
    ```

## Core concepts

### gRPC Service Definition

The service is defined in `ai.docling.serve.v1.DoclingServeService`. It mirrors the REST API but adds gRPC-specific capabilities like server-streaming for status monitoring.

### Strong Typing with Protobuf

Unlike the REST API which relies on JSON, this module provides a 1:1 Protobuf mapping of the `DoclingDocument` schema. This ensures zero data loss and maximum type safety across the gRPC boundary.

### Watch RPCs (Server-Side Polling)

One of the key advantages of the gRPC implementation is the "Watch" pattern. Instead of the client manually polling the status of an asynchronous task, the client can call a "Watch" RPC:

- `WatchConvertSource`
- `WatchChunkHierarchicalSource`
- `WatchChunkHybridSource`

The gRPC server will submit the task, manage the internal poll loop, and stream status updates back to the client until the task reaches a terminal state (Success or Failure).

### A Quick Note on the Versioning Packaging

gRPC services are defined in separate packages (e.g., `ai.docling.serve.v1`) to allow for independent versioning and evolution of the gRPC contract. This means that while the underlying `DoclingServeApi` may evolve, the gRPC service can maintain backward compatibility or introduce breaking changes in a controlled manner.

This is a common practice in gRPC development.  It allows you to retain your previous records and avoid breaking changes when you upgrade the gRPC client while still maintaining a 1:1 mapping of the REST API.

## Design Philosophy: REST vs. gRPC Best Practices

While this module provides a functional 1:1 mapping of the Docling Serve REST API, the gRPC implementation intentionally follows [Protobuf Definition Guide](https://protobuf.dev/reference/protobuf/google.protobuf/) and [Buf](https://buf.build/docs/lint/overview/) linting standards.

### Logical Mapping - gRPC for gRPC and REST for REST
Many gRPC implementations often overlook the long-term maintenance costs of using domain entities or shared models directly as RPC request/response types. While this seems convenient initially, it can lead to significant technical debt and confusion:
- **Breaking Changes:** If a shared domain model changes, every RPC using it potentially breaks.
- **Strict Linting:** Industry-standard tools would flag request/response messages that are reused across different RPCs as a violation of best practices.
- **Future-Proofing:** Distinct messages allow you to add fields to one request without cluttering every other unrelated call.
- **Protocol Conventions:** gRPC and REST have different serialization and versioning paradigms; a 1:1 binary-to-JSON bridge rarely honors both equally.

In large-scale pipeline processors, Protobuf messages are frequently used to represent the domain model and are often committed to long-term storage or S3 archives. Adhering to these gRPC standards ensures that your archived records remain readable and that downstream integrations are insulated from breaking changes, avoiding the massive overhead of data conversion projects.

As long as the mappings between APIs still honor a 1:1 binary-to-JSON bridge, it's best to avoid the trap of a "pure" 1:1 mapping.

### Unique Request/Response Wrappers
Every RPC in `DoclingServeService` has its own dedicated message pair (e.g., `ConvertSourceRequest` and `ConvertSourceResponse`). These messages *wrap* the underlying domain types (like `ConvertDocumentRequest`). This aligns with gRPC architectural patterns which favor encapsulation and independence of service methods.

This "wrapper" pattern allows the gRPC contract to remain stable and lint-clean even if the underlying Java models or REST payloads undergo minor changes. It ensures that this gRPC client is a first-class citizen in a professional service-oriented architecture, rather than a "lazy" bridge.

## Usage Examples

### Server-Side Setup
Below is a conceptual example of how to host the gRPC service. This service acts as a bridge, accepting gRPC requests and delegating them to the underlying REST client.

```java
import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.grpc.DoclingServeGrpcService;
import io.grpc.ServerBuilder;

// 1. Create the underlying REST client (bridge to Docling Serve)
DoclingServeApi restClient = DoclingServeApi.builder()
    .baseUrl("http://localhost:8000")
    .build();

// 2. Instantiate the gRPC service implementation
DoclingServeGrpcService grpcService = new DoclingServeGrpcService(restClient);

// 3. Start the gRPC server
var server = ServerBuilder.forPort(9000)
    .addService(grpcService)
    .build()
    .start();
```

### Client-Side Call
When calling the service from a gRPC client, notice how the domain request (`ConvertDocumentRequest`) is wrapped in a specific RPC request (`ConvertSourceRequest`). This follows the design philosophy of method independence.

```java
import ai.docling.serve.v1.ConvertDocumentRequest;
import ai.docling.serve.v1.ConvertDocumentResponse;
import ai.docling.serve.v1.ConvertSourceRequest;
import ai.docling.serve.v1.ConvertSourceResponse;
import ai.docling.serve.v1.DoclingServeServiceGrpc;
import ai.docling.serve.v1.HttpSource;
import ai.docling.serve.v1.Source;
import io.grpc.ManagedChannelBuilder;

// 1. Create a channel and a blocking stub
var channel = ManagedChannelBuilder.forAddress("localhost", 9000)
    .usePlaintext()
    .build();
var stub = DoclingServeServiceGrpc.newBlockingStub(channel);

// 2. Build the domain-level request
var docRequest = ConvertDocumentRequest.newBuilder()
    .addSources(Source.newBuilder()
        .setHttp(HttpSource.newBuilder()
            .setUrl("https://arxiv.org/pdf/2408.09869")
            .build())
        .build())
    .build();

// 3. Wrap it in the RPC-specific request message
var rpcRequest = ConvertSourceRequest.newBuilder()
    .setRequest(docRequest)
    .build();

// 4. Execute the call
ConvertSourceResponse rpcResponse = stub.convertSource(rpcRequest);

// 5. Access the wrapped domain-level response
ConvertDocumentResponse docResponse = rpcResponse.getResponse();
System.out.println("Status: " + docResponse.getStatus());
```

## Streaming Placeholder

The `ConvertSourceStream` RPC is currently implemented as a **logical stream**. 

While the underlying Docling backend is currently synchronous (processing all sources in a single batch), the gRPC interface is designed to be "true-streaming" ready. This means that as soon as the Docling backend supports per-document event emission, this method will be updated to emit responses as each document completes, without requiring any changes to the gRPC service contract.

This design future-proofs your integrations and allows for a more responsive user experience as the Docling ecosystem evolves.
