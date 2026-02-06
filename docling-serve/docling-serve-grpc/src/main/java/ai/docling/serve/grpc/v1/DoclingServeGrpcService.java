package ai.docling.serve.grpc.v1;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.grpc.v1.mapping.ServeApiMapper;
import ai.docling.serve.v1.*;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * gRPC service implementation for DoclingServe.
 * <p>
 * This service wraps the {@link DoclingServeApi} (REST client) and provides
 * a gRPC interface. All RPCs are implemented as proxies:
 * proto request → Java API → REST → Java response → proto response.
 * <p>
 * Includes Watch RPCs that leverage gRPC server-streaming to internally manage
 * the poll loop, streaming each status update to the client until completion.
 * <p>
 * Adheres to Buf linting by using unique request/response wrappers for every RPC.
 */
public class DoclingServeGrpcService extends DoclingServeServiceGrpc.DoclingServeServiceImplBase {

  private static final Logger LOG = LoggerFactory.getLogger(DoclingServeGrpcService.class);
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration DEFAULT_POLL_TIMEOUT = Duration.ofMinutes(5);

  private final DoclingServeApi api;
  private final Duration pollInterval;
  private final Duration pollTimeout;

  public DoclingServeGrpcService(DoclingServeApi api) {
    this(api, DEFAULT_POLL_INTERVAL, DEFAULT_POLL_TIMEOUT);
  }

  public DoclingServeGrpcService(DoclingServeApi api, Duration pollInterval, Duration pollTimeout) {
    this.api = api;
    this.pollInterval = pollInterval;
    this.pollTimeout = pollTimeout;
  }

  // ==================== Health ====================

  @Override
  public void health(HealthRequest request, StreamObserver<HealthResponse> responseObserver) {
    try {
      var javaResponse = api.health();
      responseObserver.onNext(ServeApiMapper.toProto(javaResponse));
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("Health check failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  // ==================== Convert ====================

  @Override
  public void convertSource(
      ConvertSourceRequest request,
      StreamObserver<ConvertSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      ConvertDocumentResponse javaResponse = api.convertSource(javaRequest);
      responseObserver.onNext(ConvertSourceResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ConvertSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void convertSourceAsync(
      ConvertSourceAsyncRequest request,
      StreamObserver<ConvertSourceAsyncResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var javaResponse = api.submitConvertSource(javaRequest);
      responseObserver.onNext(ConvertSourceAsyncResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ConvertSourceAsync failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void convertSourceStream(
      ConvertSourceStreamRequest request,
      StreamObserver<ConvertSourceStreamResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      // NOTE for Demo: This is currently a "logical" stream.
      ConvertDocumentResponse javaResponse = api.convertSource(javaRequest);
      responseObserver.onNext(ConvertSourceStreamResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ConvertSourceStream failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  // ==================== Chunk ====================

  @Override
  public void chunkHierarchicalSource(
      ChunkHierarchicalSourceRequest request,
      StreamObserver<ChunkHierarchicalSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      ChunkDocumentResponse javaResponse = api.chunkSourceWithHierarchicalChunker(javaRequest);
      responseObserver.onNext(ChunkHierarchicalSourceResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ChunkHierarchicalSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void chunkHybridSource(
      ChunkHybridSourceRequest request,
      StreamObserver<ChunkHybridSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      ChunkDocumentResponse javaResponse = api.chunkSourceWithHybridChunker(javaRequest);
      responseObserver.onNext(ChunkHybridSourceResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ChunkHybridSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void chunkHierarchicalSourceAsync(
      ChunkHierarchicalSourceAsyncRequest request,
      StreamObserver<ChunkHierarchicalSourceAsyncResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var javaResponse = api.submitChunkHierarchicalSource(javaRequest);
      responseObserver.onNext(ChunkHierarchicalSourceAsyncResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ChunkHierarchicalSourceAsync failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void chunkHybridSourceAsync(
      ChunkHybridSourceAsyncRequest request,
      StreamObserver<ChunkHybridSourceAsyncResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var javaResponse = api.submitChunkHybridSource(javaRequest);
      responseObserver.onNext(ChunkHybridSourceAsyncResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ChunkHybridSourceAsync failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  // ==================== Task ====================

  @Override
  public void pollTaskStatus(
      PollTaskStatusRequest request,
      StreamObserver<PollTaskStatusResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      TaskStatusPollResponse javaResponse = api.pollTaskStatus(javaRequest);
      responseObserver.onNext(PollTaskStatusResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("PollTaskStatus failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void getConvertResult(
      GetConvertResultRequest request,
      StreamObserver<GetConvertResultResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      ConvertDocumentResponse javaResponse = api.convertTaskResult(javaRequest);
      responseObserver.onNext(GetConvertResultResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("GetConvertResult failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void getChunkResult(
      GetChunkResultRequest request,
      StreamObserver<GetChunkResultResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      ChunkDocumentResponse javaResponse = api.chunkTaskResult(javaRequest);
      responseObserver.onNext(GetChunkResultResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("GetChunkResult failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  // ==================== Watch (streaming async) ====================

  @Override
  public void watchConvertSource(
      WatchConvertSourceRequest request,
      StreamObserver<WatchConvertSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var initialStatus = api.submitConvertSource(javaRequest);
      pollAndStream(initialStatus, responseObserver, "WatchConvertSource", status ->
          WatchConvertSourceResponse.newBuilder().setResponse(ServeApiMapper.toProto(status)).build());
    } catch (Exception e) {
      LOG.error("WatchConvertSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void watchChunkHierarchicalSource(
      WatchChunkHierarchicalSourceRequest request,
      StreamObserver<WatchChunkHierarchicalSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var initialStatus = api.submitChunkHierarchicalSource(javaRequest);
      pollAndStream(initialStatus, responseObserver, "WatchChunkHierarchicalSource", status ->
          WatchChunkHierarchicalSourceResponse.newBuilder().setResponse(ServeApiMapper.toProto(status)).build());
    } catch (Exception e) {
      LOG.error("WatchChunkHierarchicalSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void watchChunkHybridSource(
      WatchChunkHybridSourceRequest request,
      StreamObserver<WatchChunkHybridSourceResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request.getRequest());
      var initialStatus = api.submitChunkHybridSource(javaRequest);
      pollAndStream(initialStatus, responseObserver, "WatchChunkHybridSource", status ->
          WatchChunkHybridSourceResponse.newBuilder().setResponse(ServeApiMapper.toProto(status)).build());
    } catch (Exception e) {
      LOG.error("WatchChunkHybridSource failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  private <T> void pollAndStream(
      TaskStatusPollResponse initialStatus,
      StreamObserver<T> responseObserver,
      String rpcName,
      java.util.function.Function<TaskStatusPollResponse, T> mapper) {

    if (io.grpc.Context.current().isCancelled()) {
      LOG.info("{}: client cancelled the request early", rpcName);
      return;
    }

    responseObserver.onNext(mapper.apply(initialStatus));
    LOG.info("{}: task {} submitted with status {}",
        rpcName, initialStatus.getTaskId(), initialStatus.getTaskStatus());

    if (isTerminal(initialStatus.getTaskStatus())) {
      responseObserver.onCompleted();
      return;
    }

    var taskId = initialStatus.getTaskId();
    var pollRequest = TaskStatusPollRequest.builder()
        .taskId(taskId)
        .build();

    long deadline = System.currentTimeMillis() + pollTimeout.toMillis();

    while (System.currentTimeMillis() < deadline) {
      if (io.grpc.Context.current().isCancelled()) {
        LOG.info("{}: client cancelled the request", rpcName);
        return;
      }

      try {
        Thread.sleep(pollInterval.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        responseObserver.onError(
            Status.CANCELLED.withDescription("Polling interrupted").asRuntimeException());
        return;
      }

      TaskStatusPollResponse status;
      try {
        status = api.pollTaskStatus(pollRequest);
      } catch (Exception e) {
        LOG.error("{}: polling failed for task {}", rpcName, taskId, e);
        responseObserver.onError(
            Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        return;
      }

      responseObserver.onNext(mapper.apply(status));
      LOG.debug("{}: task {} status: {}", rpcName, taskId, status.getTaskStatus());

      if (isTerminal(status.getTaskStatus())) {
        responseObserver.onCompleted();
        return;
      }
    }

    LOG.warn("{}: task {} timed out after {}", rpcName, taskId, pollTimeout);
    responseObserver.onError(
        Status.DEADLINE_EXCEEDED
            .withDescription("Task %s did not complete within %s".formatted(taskId, pollTimeout))
            .asRuntimeException());
  }

  private static boolean isTerminal(TaskStatus status) {
    return status == TaskStatus.SUCCESS || status == TaskStatus.FAILURE;
  }

  // ==================== Clear ====================

  @Override
  public void clearConverters(
      ClearConvertersRequest request,
      StreamObserver<ClearConvertersResponse> responseObserver) {
    try {
      var javaRequest = ai.docling.serve.api.clear.request.ClearConvertersRequest.builder().build();
      var javaResponse = api.clearConverters(javaRequest);
      responseObserver.onNext(ClearConvertersResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ClearConverters failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }

  @Override
  public void clearResults(
      ClearResultsRequest request,
      StreamObserver<ClearResultsResponse> responseObserver) {
    try {
      var javaRequest = ServeApiMapper.toJava(request);
      var javaResponse = api.clearResults(javaRequest);
      responseObserver.onNext(ClearResultsResponse.newBuilder()
          .setResponse(ServeApiMapper.toProto(javaResponse))
          .build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      LOG.error("ClearResults failed", e);
      responseObserver.onError(
          Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
    }
  }
}