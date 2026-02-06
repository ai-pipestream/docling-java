package ai.docling.serve.grpc.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.HierarchicalChunkDocumentRequest;
import ai.docling.serve.api.chunk.request.HybridChunkDocumentRequest;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.clear.request.ClearResultsRequest;
import ai.docling.serve.api.clear.response.ClearResponse;
import ai.docling.serve.api.convert.request.ConvertDocumentRequest;
import ai.docling.serve.api.convert.response.ConvertDocumentResponse;
import ai.docling.serve.api.convert.response.DocumentResponse;
import ai.docling.serve.api.health.HealthCheckResponse;
import ai.docling.serve.api.task.request.TaskResultRequest;
import ai.docling.serve.api.task.request.TaskStatusPollRequest;
import ai.docling.serve.api.task.response.TaskStatus;
import ai.docling.serve.api.task.response.TaskStatusPollResponse;
import ai.docling.serve.grpc.v1.mapping.ServeApiMapper;
import ai.docling.serve.v1.*;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoclingServeGrpcServiceTest {

    @Mock
    private DoclingServeApi api;

    private ManagedChannel channel;
    private DoclingServeServiceGrpc.DoclingServeServiceBlockingStub blockingStub;
    private String serverName;

    @BeforeEach
    void setUp() throws Exception {
        serverName = InProcessServerBuilder.generateName();
        var server = InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(new DoclingServeGrpcService(api,
                Duration.ofMillis(50), Duration.ofSeconds(5)))
            .build()
            .start();
        channel = InProcessChannelBuilder.forName(serverName)
            .directExecutor()
            .build();
        blockingStub = DoclingServeServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @Nested
    class HealthTests {

        @Test
        void healthReturnsStatus() {
            when(api.health()).thenReturn(
                HealthCheckResponse.builder().status("ok").build());

            var response = blockingStub.health(
                HealthRequest.getDefaultInstance());

            assertThat(response.getStatus()).isEqualTo("ok");
            verify(api).health();
        }

        @Test
        void healthPropagatesErrorAsInternal() {
            when(api.health()).thenThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> blockingStub.health(
                    HealthRequest.getDefaultInstance()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Connection refused");
        }
    }

    @Nested
    class ConvertTests {

        @Test
        void convertSourceDelegatesToApi() {
            var apiResponse = ConvertDocumentResponse.builder()
                .document(DocumentResponse.builder()
                    .filename("test.pdf")
                    .markdownContent("# Test")
                    .build())
                .status("success")
                .build();

            when(api.convertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(apiResponse);

            var request = ConvertSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.ConvertDocumentRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                            .setBase64String("dGVzdA==")
                            .setFilename("test.pdf")
                            .build())
                        .build())
                    .build())
                .build();

            var response = blockingStub.convertSource(request);

            assertThat(response.getResponse().getDocument().getFilename()).isEqualTo("test.pdf");
            assertThat(response.getResponse().getDocument().getMdContent()).isEqualTo("# Test");
            assertThat(response.getResponse().getStatus()).isEqualTo("success");
            verify(api).convertSource(any(ConvertDocumentRequest.class));
        }

        @Test
        void convertSourcePropagatesErrors() {
            when(api.convertSource(any(ConvertDocumentRequest.class)))
                .thenThrow(new RuntimeException("Conversion failed"));

            assertThatThrownBy(() -> blockingStub.convertSource(
                    ConvertSourceRequest.getDefaultInstance()))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Conversion failed");
        }

        @Test
        void convertSourceAsyncReturnsTaskStatus() {
            var taskResponse = TaskStatusPollResponse.builder()
                .taskId("task-123")
                .taskStatus(TaskStatus.STARTED)
                .taskPosition(0L)
                .build();

            when(api.submitConvertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(taskResponse);

            var response = blockingStub.convertSourceAsync(
                ConvertSourceAsyncRequest.getDefaultInstance());

            assertThat(response.getResponse().getTaskId()).isEqualTo("task-123");
            assertThat(response.getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED);
            verify(api).submitConvertSource(any(ConvertDocumentRequest.class));
        }

        @Test
        void convertSourceStreamReturnsResponse() {
            var apiResponse = ConvertDocumentResponse.builder()
                .document(DocumentResponse.builder()
                    .filename("test.pdf")
                    .markdownContent("# Stream")
                    .build())
                .build();

            when(api.convertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(apiResponse);

            var responses = blockingStub.convertSourceStream(
                ConvertSourceStreamRequest.getDefaultInstance());

            assertThat(responses.hasNext()).isTrue();
            var response = responses.next();
            assertThat(response.getResponse().getDocument().getMdContent()).isEqualTo("# Stream");
            assertThat(responses.hasNext()).isFalse();
        }
    }

    @Nested
    class ChunkTests {

        @Test
        void chunkHierarchicalSourceDelegatesToApi() {
            var apiResponse = ChunkDocumentResponse.builder()
                .processingTime(1.0)
                .build();

            when(api.chunkSourceWithHierarchicalChunker(
                    any(HierarchicalChunkDocumentRequest.class)))
                .thenReturn(apiResponse);

            var request = ChunkHierarchicalSourceRequest.newBuilder()
                .setRequest(ai.docling.serve.v1.HierarchicalChunkRequest.newBuilder()
                    .addSources(ai.docling.serve.v1.Source.newBuilder()
                        .setFile(ai.docling.serve.v1.FileSource.newBuilder()
                            .setBase64String("dGVzdA==")
                            .setFilename("test.pdf")
                            .build())
                        .build())
                    .build())
                .build();

            var response = blockingStub.chunkHierarchicalSource(request);

            assertThat(response.getResponse().getProcessingTime()).isEqualTo(1.0);
            verify(api).chunkSourceWithHierarchicalChunker(
                any(HierarchicalChunkDocumentRequest.class));
        }

        @Test
        void chunkHybridSourceDelegatesToApi() {
            var apiResponse = ChunkDocumentResponse.builder()
                .processingTime(0.5)
                .build();

            when(api.chunkSourceWithHybridChunker(any(HybridChunkDocumentRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.chunkHybridSource(
                ChunkHybridSourceRequest.getDefaultInstance());

            assertThat(response.getResponse().getProcessingTime()).isEqualTo(0.5);
            verify(api).chunkSourceWithHybridChunker(
                any(HybridChunkDocumentRequest.class));
        }

        @Test
        void chunkHierarchicalSourceAsyncReturnsTaskStatus() {
            var taskResponse = TaskStatusPollResponse.builder()
                .taskId("chunk-task-1")
                .taskStatus(TaskStatus.PENDING)
                .taskPosition(1L)
                .build();

            when(api.submitChunkHierarchicalSource(
                    any(HierarchicalChunkDocumentRequest.class)))
                .thenReturn(taskResponse);

            var response = blockingStub.chunkHierarchicalSourceAsync(
                ChunkHierarchicalSourceAsyncRequest.getDefaultInstance());

            assertThat(response.getResponse().getTaskId()).isEqualTo("chunk-task-1");
            assertThat(response.getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_PENDING);
            verify(api).submitChunkHierarchicalSource(
                any(HierarchicalChunkDocumentRequest.class));
        }

        @Test
        void chunkHybridSourceAsyncReturnsTaskStatus() {
            var taskResponse = TaskStatusPollResponse.builder()
                .taskId("chunk-task-2")
                .taskStatus(TaskStatus.STARTED)
                .taskPosition(0L)
                .build();

            when(api.submitChunkHybridSource(any(HybridChunkDocumentRequest.class)))
                .thenReturn(taskResponse);

            var response = blockingStub.chunkHybridSourceAsync(
                ChunkHybridSourceAsyncRequest.getDefaultInstance());

            assertThat(response.getResponse().getTaskId()).isEqualTo("chunk-task-2");
            assertThat(response.getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED);
            verify(api).submitChunkHybridSource(
                any(HybridChunkDocumentRequest.class));
        }
    }

    @Nested
    class TaskTests {

        @Test
        void pollTaskStatusDelegatesToApi() {
            var apiResponse = TaskStatusPollResponse.builder()
                .taskId("task-123")
                .taskStatus(TaskStatus.STARTED)
                .taskPosition(2L)
                .build();

            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.pollTaskStatus(
                PollTaskStatusRequest.newBuilder()
                    .setRequest(ai.docling.serve.v1.TaskStatusPollRequest.newBuilder()
                        .setTaskId("task-123")
                        .build())
                    .build());

            assertThat(response.getResponse().getTaskId()).isEqualTo("task-123");
            assertThat(response.getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED);
            assertThat(response.getResponse().getTaskPosition()).isEqualTo(2);
        }

        @Test
        void getConvertResultDelegatesToApi() {
            var apiResponse = ConvertDocumentResponse.builder()
                .document(DocumentResponse.builder()
                    .filename("result.pdf")
                    .build())
                .build();

            when(api.convertTaskResult(any(TaskResultRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.getConvertResult(
                GetConvertResultRequest.newBuilder()
                    .setRequest(ai.docling.serve.v1.TaskResultRequest.newBuilder()
                        .setTaskId("task-456")
                        .build())
                    .build());

            assertThat(response.getResponse().getDocument().getFilename()).isEqualTo("result.pdf");
        }

        @Test
        void getChunkResultDelegatesToApi() {
            var apiResponse = ChunkDocumentResponse.builder()
                .processingTime(2.0)
                .build();

            when(api.chunkTaskResult(any(TaskResultRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.getChunkResult(
                GetChunkResultRequest.newBuilder()
                    .setRequest(ai.docling.serve.v1.TaskResultRequest.newBuilder()
                        .setTaskId("task-789")
                        .build())
                    .build());

            assertThat(response.getResponse().getProcessingTime()).isEqualTo(2.0);
        }
    }

    @Nested
    class WatchTests {

        @Test
        void watchConvertSourceStreamsStatusUpdates() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-1")
                .taskStatus(TaskStatus.PENDING)
                .taskPosition(2L)
                .build();
            var polledStarted = TaskStatusPollResponse.builder()
                .taskId("watch-1")
                .taskStatus(TaskStatus.STARTED)
                .taskPosition(1L)
                .build();
            var polledSuccess = TaskStatusPollResponse.builder()
                .taskId("watch-1")
                .taskStatus(TaskStatus.SUCCESS)
                .taskPosition(0L)
                .build();

            when(api.submitConvertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(submitResponse);
            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenReturn(polledStarted, polledSuccess);

            var responses = new ArrayList<WatchConvertSourceResponse>();
            var iterator = blockingStub.watchConvertSource(
                WatchConvertSourceRequest.getDefaultInstance());
            iterator.forEachRemaining(responses::add);

            assertThat(responses).hasSize(3);
            assertThat(responses.get(0).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_PENDING);
            assertThat(responses.get(1).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED);
            assertThat(responses.get(2).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS);
            verify(api).submitConvertSource(any(ConvertDocumentRequest.class));
            verify(api, times(2)).pollTaskStatus(any(TaskStatusPollRequest.class));
        }

        @Test
        void watchChunkHierarchicalSourceStreamsStatusUpdates() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-h-1")
                .taskStatus(TaskStatus.PENDING)
                .build();
            var polledSuccess = TaskStatusPollResponse.builder()
                .taskId("watch-h-1")
                .taskStatus(TaskStatus.SUCCESS)
                .build();

            when(api.submitChunkHierarchicalSource(
                    any(HierarchicalChunkDocumentRequest.class)))
                .thenReturn(submitResponse);
            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenReturn(polledSuccess);

            var responses = new ArrayList<WatchChunkHierarchicalSourceResponse>();
            blockingStub.watchChunkHierarchicalSource(
                WatchChunkHierarchicalSourceRequest.getDefaultInstance())
                .forEachRemaining(responses::add);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_PENDING);
            assertThat(responses.get(1).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS);
            verify(api).submitChunkHierarchicalSource(
                any(HierarchicalChunkDocumentRequest.class));
            verify(api, times(1)).pollTaskStatus(any(TaskStatusPollRequest.class));
        }

        @Test
        void watchChunkHybridSourceStreamsStatusUpdates() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-y-1")
                .taskStatus(TaskStatus.STARTED)
                .build();
            var polledSuccess = TaskStatusPollResponse.builder()
                .taskId("watch-y-1")
                .taskStatus(TaskStatus.SUCCESS)
                .build();

            when(api.submitChunkHybridSource(any(HybridChunkDocumentRequest.class)))
                .thenReturn(submitResponse);
            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenReturn(polledSuccess);

            var responses = new ArrayList<WatchChunkHybridSourceResponse>();
            blockingStub.watchChunkHybridSource(
                WatchChunkHybridSourceRequest.getDefaultInstance())
                .forEachRemaining(responses::add);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_STARTED);
            assertThat(responses.get(1).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS);
            verify(api).submitChunkHybridSource(any(HybridChunkDocumentRequest.class));
        }

        @Test
        void watchCompletesImmediatelyWhenSubmitReturnsSuccess() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-imm")
                .taskStatus(TaskStatus.SUCCESS)
                .build();

            when(api.submitConvertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(submitResponse);

            var responses = new ArrayList<WatchConvertSourceResponse>();
            blockingStub.watchConvertSource(
                WatchConvertSourceRequest.getDefaultInstance())
                .forEachRemaining(responses::add);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_SUCCESS);
            verify(api).submitConvertSource(any(ConvertDocumentRequest.class));
            verify(api, times(0)).pollTaskStatus(any(TaskStatusPollRequest.class));
        }

        @Test
        void watchCompletesOnFailureStatus() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-fail")
                .taskStatus(TaskStatus.PENDING)
                .build();
            var polledFailure = TaskStatusPollResponse.builder()
                .taskId("watch-fail")
                .taskStatus(TaskStatus.FAILURE)
                .build();

            when(api.submitConvertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(submitResponse);
            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenReturn(polledFailure);

            var responses = new ArrayList<WatchConvertSourceResponse>();
            blockingStub.watchConvertSource(
                WatchConvertSourceRequest.getDefaultInstance())
                .forEachRemaining(responses::add);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_PENDING);
            assertThat(responses.get(1).getResponse().getTaskStatus())
                .isEqualTo(ai.docling.serve.v1.TaskStatus.TASK_STATUS_FAILURE);
        }

        @Test
        void watchPropagatesPollError() {
            var submitResponse = TaskStatusPollResponse.builder()
                .taskId("watch-err")
                .taskStatus(TaskStatus.PENDING)
                .build();

            when(api.submitConvertSource(any(ConvertDocumentRequest.class)))
                .thenReturn(submitResponse);
            when(api.pollTaskStatus(any(TaskStatusPollRequest.class)))
                .thenThrow(new RuntimeException("Poll connection refused"));

            assertThatThrownBy(() -> {
                blockingStub.watchConvertSource(
                    WatchConvertSourceRequest.getDefaultInstance())
                    .forEachRemaining(r -> {});
            })
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Poll connection refused");
        }
    }

    @Nested
    class ClearTests {

        @Test
        void clearConvertersDelegatesToApi() {
            var apiResponse = ClearResponse.builder().status("cleared").build();

            when(api.clearConverters(
                    any(ai.docling.serve.api.clear.request.ClearConvertersRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.clearConverters(
                ClearConvertersRequest.getDefaultInstance());

            assertThat(response.getResponse().getStatus()).isEqualTo("cleared");
        }

        @Test
        void clearResultsDelegatesToApi() {
            var apiResponse = ClearResponse.builder().status("cleared").build();

            when(api.clearResults(any(ClearResultsRequest.class)))
                .thenReturn(apiResponse);

            var response = blockingStub.clearResults(
                ai.docling.serve.v1.ClearResultsRequest.newBuilder()
                    .setOlderThan(60.0f)
                    .build());

            assertThat(response.getResponse().getStatus()).isEqualTo("cleared");
        }
    }
}